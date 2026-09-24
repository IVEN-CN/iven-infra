package io.github.ivencn.infra.rbac.validate;

import io.github.ivencn.infra.security.annotation.RequiresPermission;
import io.github.ivencn.infra.security.principal.AccessLevel;
import lombok.Getter;
import org.springframework.web.method.HandlerMethod;

/**
 * 权限解析工具类，处理类级与方法级权限注解的优先级规则（方法级 > 类级）。
 */
public final class PermissionResolver {

    private PermissionResolver() {
    }

    /**
     * 权限信息包装类。
     */
    public static class PermissionInfo {
        @Getter
        private final String value;
        @Getter
        private final String name;
        @Getter
        private final AccessLevel access;
        private final boolean hasPermission;

        public PermissionInfo(String value, String name, AccessLevel access, boolean hasPermission) {
            this.value = value;
            this.name = name;
            this.access = access;
            this.hasPermission = hasPermission;
        }

        public boolean hasPermission() {
            return hasPermission;
        }
    }

    /**
     * 解析 HandlerMethod 的权限信息，优先级: 方法级 > 类级。
     *
     * @return 权限信息（无注解时返回 hasPermission=false）
     */
    public static PermissionInfo resolve(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return new PermissionInfo(null, null, null, false);
        }

        RequiresPermission classPermission = handlerMethod.getBeanType().getAnnotation(RequiresPermission.class);
        RequiresPermission methodPermission = handlerMethod.getMethodAnnotation(RequiresPermission.class);

        if (methodPermission != null) {
            PermissionValidator.validate(
                    methodPermission,
                    handlerMethod.getBeanType().getName(),
                    handlerMethod.getMethod().getName());
            return new PermissionInfo(methodPermission.value(), methodPermission.name(), methodPermission.access(),
                    true);
        }

        if (classPermission != null) {
            PermissionValidator.validate(classPermission, handlerMethod.getBeanType().getName(), "class-level");
            return new PermissionInfo(classPermission.value(), classPermission.name(), classPermission.access(), true);
        }

        return new PermissionInfo(null, null, null, false);
    }

    /**
     * 检查是否有权限注解（类级或方法级）。
     */
    public static boolean hasPermission(HandlerMethod handlerMethod) {
        if (handlerMethod == null) {
            return false;
        }
        return handlerMethod.getMethodAnnotation(RequiresPermission.class) != null
                || handlerMethod.getBeanType().getAnnotation(RequiresPermission.class) != null;
    }
}
