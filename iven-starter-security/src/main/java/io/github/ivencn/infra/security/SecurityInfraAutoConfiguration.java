package io.github.ivencn.infra.security;

import io.github.ivencn.infra.security.annotation.AccessLevelFallbackAspect;
import io.github.ivencn.infra.security.auth.AuthTokenServiceImpl;
import io.github.ivencn.infra.security.cookie.CookieProperties;
import io.github.ivencn.infra.security.cookie.CookieService;
import io.github.ivencn.infra.security.cookie.CookieServiceImpl;
import io.github.ivencn.infra.security.csrf.CsrfProperties;
import io.github.ivencn.infra.security.csrf.CsrfTokenService;
import io.github.ivencn.infra.security.csrf.CsrfTokenServiceImpl;
import io.github.ivencn.infra.security.http.FailAuthEntryPoint;
import io.github.ivencn.infra.security.jwt.JwtProperties;
import io.github.ivencn.infra.security.jwt.JwtService;
import io.github.ivencn.infra.security.oauth.OAuthStateStore;
import io.github.ivencn.infra.security.ratelimit.AnonymousUploadRateLimiter;
import io.github.ivencn.infra.security.ratelimit.RateLimitAspect;
import io.github.ivencn.infra.security.spi.UserDetailsLoader;
import io.github.ivencn.infra.core.banner.InfraBannerPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * iven-starter-security 自动装配。开关：iven.security.enabled（默认 true）。
 *
 * <p>
 * 过滤器（JwtAuthenticationFilter、CsrfTokenFilter）由应用在 SecurityFilterChain
 * 中自行装配；本配置提供服务层 bean。用户信息取数需要应用提供 {@link UserDetailsLoader} 实现。
 * </p>
 */
@AutoConfiguration(after = { RedisAutoConfiguration.class,
        io.github.ivencn.infra.web.WebInfraAutoConfiguration.class })
@ConditionalOnProperty(name = "iven.security.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class, CsrfProperties.class})
public class SecurityInfraAutoConfiguration {

    @PostConstruct
    void printBanner() {
        InfraBannerPrinter.printOnce();
    }

    @Bean
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public AuthTokenServiceImpl authTokenService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        return new AuthTokenServiceImpl(redisTemplate, jwtProperties);
    }

    @Bean
    public CookieService cookieService(CookieProperties cookieProperties) {
        return new CookieServiceImpl(cookieProperties);
    }

    @Bean
    public FailAuthEntryPoint failAuthEntryPoint(CookieService cookieService) {
        return new FailAuthEntryPoint(cookieService);
    }

    @Bean
    public CsrfTokenService csrfTokenService(CookieProperties cookieProperties, CsrfProperties csrfProperties) {
        return new CsrfTokenServiceImpl(cookieProperties, csrfProperties);
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public RateLimitAspect rateLimitAspect(StringRedisTemplate redisTemplate) {
        return new RateLimitAspect(redisTemplate);
    }

    @Bean
    public AnonymousUploadRateLimiter anonymousUploadRateLimiter() {
        return new AnonymousUploadRateLimiter();
    }

    @Bean
    @ConditionalOnBean(StringRedisTemplate.class)
    public OAuthStateStore oauthStateStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new OAuthStateStore(redisTemplate, objectMapper);
    }

    /**
     * RBAC 关闭时的降级切面：只执行 PUBLIC/AUTHENTICATED/PROTECTED 登录态门槛。
     */
    @Bean
    @ConditionalOnProperty(name = "iven.rbac.enabled", havingValue = "false", matchIfMissing = true)
    public AccessLevelFallbackAspect accessLevelFallbackAspect(
            @org.springframework.beans.factory.annotation.Value("${iven.rbac.protected-fallback:authenticated}") String protectedFallback) {
        return new AccessLevelFallbackAspect(protectedFallback);
    }
}
