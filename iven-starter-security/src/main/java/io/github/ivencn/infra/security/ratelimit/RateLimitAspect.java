package io.github.ivencn.infra.security.ratelimit;

import io.github.ivencn.infra.core.exception.BizException;
import io.github.ivencn.infra.web.util.IpUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Duration;

/**
 * IP 限频切面。
 *
 * <p>
 * 拦截 {@link RateLimit} 注解方法，基于 Redis SETNX + TTL 实现按 IP 的请求间隔限频。 每个接口独立计算，同一
 * IP 在接口 A 被限不影响接口 B。Redis 不可用时降级放行。
 * </p>
 */
@Aspect
public class RateLimitAspect {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitAspect.class);
    private static final String KEY_PREFIX = "rate_limit:";

    private final StringRedisTemplate redisTemplate;

    public RateLimitAspect(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint pjp, RateLimit rateLimit) throws Throwable {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return pjp.proceed();
        }

        String clientIp = IpUtils.getClientIp(request);
        String key = buildKey(clientIp, request.getMethod(), request.getRequestURI());

        try {
            Boolean allowed = redisTemplate.opsForValue()
                    .setIfAbsent(key, "1", Duration.ofSeconds(rateLimit.interval()));

            if (Boolean.FALSE.equals(allowed)) {
                logger.warn("请求过于频繁 - ip={}, method={}", clientIp, key);
                throw new BizException(HttpStatus.TOO_MANY_REQUESTS, "请求过于频繁，请稍后再试");
            }
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Redis 限频异常，降级放行 - ip={}, key={}", clientIp, key, e);
        }

        return pjp.proceed();
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String buildKey(String ip, String httpMethod, String uri) {
        return KEY_PREFIX + ip + ":" + httpMethod + ":" + uri;
    }
}
