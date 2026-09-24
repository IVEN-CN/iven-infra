package io.github.ivencn.infra.security.annotation;

import io.github.ivencn.infra.security.principal.AccessLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限注解，用于标记 Controller 方法的访问权限。
 *
 * <p>
 * RBAC 模块开启时由 {@code PermissionAspect} 执行完整校验（含权限集合检查）；
 * RBAC 关闭时由 security 模块的降级切面只执行登录态门槛校验。
 * </p>
 */
@Target({ ElementType.METHOD, ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface RequiresPermission {

    /**
     * 权限唯一标识，格式: resource:action（如 user:create、assessment-time:view-self），
     * 必须满足正则 {@code ^[a-z-]+(:[a-z-]+)+$}。全局唯一，重复将导致启动失败。
     */
    String value();

    /**
     * 权限显示名称，用于后台管理和日志展示
     */
    String name() default "";

    /**
     * 访问级别，默认 PROTECTED（需要权限校验）
     */
    AccessLevel access() default AccessLevel.PROTECTED;

    /**
     * 是否记录审计日志（供应用侧审计切面使用）
     */
    boolean audit() default true;
}
