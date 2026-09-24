package io.github.ivencn.infra.security.annotation;

import io.github.ivencn.infra.security.principal.AccessLevel;
import io.github.ivencn.infra.security.principal.UserCTX;
import io.github.ivencn.infra.core.exception.BizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * 访问级别降级切面。
 *
 * <p>
 * 仅在 {@code iven.rbac.enabled=false} 时装配：为 {@link RequiresPermission} 注解
 * 提供基础门槛校验——PUBLIC 放行，AUTHENTICATED 与 PROTECTED 要求已登录
 * （PROTECTED 额外输出"权限未校验"告警，按 {@code iven.rbac.protected-fallback}
 * 处理，默认降级为仅登录校验）。
 * </p>
 */
@Aspect
public class AccessLevelFallbackAspect {

    private static final Logger logger = LoggerFactory.getLogger(AccessLevelFallbackAspect.class);

    private final String protectedFallback;

    public AccessLevelFallbackAspect(String protectedFallback) {
        this.protectedFallback = protectedFallback;
    }

    @Around("@annotation(requiresPermission)")
    public Object checkAccessLevel(ProceedingJoinPoint pjp, RequiresPermission requiresPermission) throws Throwable {
        switch (requiresPermission.access()) {
            case PUBLIC :
                return pjp.proceed();

            case AUTHENTICATED :
                if (!UserCTX.isAuthenticated()) {
                    throw new BizException(HttpStatus.UNAUTHORIZED, "未认证");
                }
                return pjp.proceed();

            case PROTECTED :
            default :
                if (!UserCTX.isAuthenticated()) {
                    throw new BizException(HttpStatus.UNAUTHORIZED, "未认证");
                }
                if (!"authenticated".equalsIgnoreCase(protectedFallback)) {
                    logger.error("PROTECTED 接口在 RBAC 关闭时被拒绝: {}（iven.rbac.protected-fallback=deny）",
                            requiresPermission.value());
                    throw new BizException(HttpStatus.FORBIDDEN, "RBAC 未启用");
                }
                logger.warn("RBAC 未启用，PROTECTED 接口降级为登录校验: {}", requiresPermission.value());
                return pjp.proceed();
        }
    }
}
