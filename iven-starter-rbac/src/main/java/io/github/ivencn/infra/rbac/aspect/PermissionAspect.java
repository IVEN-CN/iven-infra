package io.github.ivencn.infra.rbac.aspect;

import io.github.ivencn.infra.rbac.role.Role;
import io.github.ivencn.infra.rbac.spi.SuperAdminPolicy;
import io.github.ivencn.infra.security.annotation.RequiresPermission;
import io.github.ivencn.infra.security.principal.SecurityPrincipal;
import io.github.ivencn.infra.security.principal.UserCTX;
import io.github.ivencn.infra.core.exception.BizException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;

/**
 * 权限切面，拦截所有带 {@link RequiresPermission} 注解的方法执行权限校验。
 *
 * <p>
 * 校验顺序：超级管理员短路 → 按访问级别处理（PUBLIC 放行 / AUTHENTICATED 登录态 /
 * PROTECTED 登录态 + 权限集合检查）。
 * </p>
 */
@Aspect
public class PermissionAspect {

    private static final Logger logger = LoggerFactory.getLogger(PermissionAspect.class);

    private final SuperAdminPolicy superAdminPolicy;

    public PermissionAspect(SuperAdminPolicy superAdminPolicy) {
        this.superAdminPolicy = superAdminPolicy;
    }

    @Around("@annotation(requiresPermission)")
    public Object checkPermission(ProceedingJoinPoint pjp, RequiresPermission requiresPermission) throws Throwable {
        boolean superAdmin = isSuperAdmin();
        logger.debug("Permission check: isSuperAdmin={}, permission={}", superAdmin, requiresPermission.value());
        if (superAdmin) {
            return pjp.proceed();
        }

        switch (requiresPermission.access()) {
            case PUBLIC :
                return pjp.proceed();

            case AUTHENTICATED :
                if (!UserCTX.isAuthenticated()) {
                    logger.warn("Unauthenticated access to AUTHENTICATED endpoint: {}", requiresPermission.value());
                    throw new BizException(HttpStatus.UNAUTHORIZED, "未认证");
                }
                return pjp.proceed();

            case PROTECTED :
            default :
                if (!UserCTX.isAuthenticated()) {
                    logger.warn("Unauthenticated access to PROTECTED endpoint: {}", requiresPermission.value());
                    throw new BizException(HttpStatus.UNAUTHORIZED, "未认证");
                }
                if (!hasPermission(requiresPermission.value())) {
                    logger.warn("Access denied to {} for user", requiresPermission.value());
                    throw new BizException(HttpStatus.FORBIDDEN, "无权限");
                }
                return pjp.proceed();
        }
    }

    private boolean hasPermission(String permissionValue) {
        SecurityPrincipal<?> principal = UserCTX.getPrincipal();
        return principal != null && principal.hasPermission(permissionValue);
    }

    /**
     * 取当前主体的角色（应用装配时定型为 {@link Role}）。
     */
    private Role currentRole() {
        SecurityPrincipal<?> principal = UserCTX.getPrincipal();
        Object role = principal != null ? principal.role() : null;
        return role instanceof Role r ? r : null;
    }

    private boolean isSuperAdmin() {
        Role role = currentRole();
        if (role == null) {
            return false;
        }
        return superAdminPolicy.isSuperAdmin(role);
    }
}
