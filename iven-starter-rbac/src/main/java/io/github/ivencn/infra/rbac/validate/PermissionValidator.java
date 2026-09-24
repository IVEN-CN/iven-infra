package io.github.ivencn.infra.rbac.validate;

import io.github.ivencn.infra.security.annotation.RequiresPermission;

import java.util.regex.Pattern;

/**
 * 权限校验工具类，用于验证权限注解的格式和合法性。
 */
public final class PermissionValidator {

    /**
     * 权限值格式正则表达式，格式: resource:action（小写字母与连字符）
     */
    private static final Pattern PERMISSION_PATTERN = Pattern.compile("^[a-z-]+(:[a-z-]+)+$");

    private PermissionValidator() {
    }

    /**
     * 验证权限值格式。
     *
     * @return true 如果格式正确
     */
    public static boolean isValid(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return PERMISSION_PATTERN.matcher(value).matches();
    }

    /**
     * 验证权限注解，验证失败抛出异常。
     */
    public static void validate(RequiresPermission requiresPermission, String className, String methodName) {
        if (requiresPermission == null) {
            return;
        }
        validate(requiresPermission.value(), className, methodName);
    }

    /**
     * 直接验证权限值，验证失败抛出异常。
     */
    public static void validate(String value, String className, String methodName) {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException(
                    String.format("Permission value cannot be empty. Class: %s, Method: %s", className, methodName));
        }

        if (!isValid(value)) {
            throw new IllegalArgumentException(String.format(
                    "Invalid permission value '%s'. Must match pattern '[resource:action]' (lowercase letters only). Class: %s, Method: %s",
                    value,
                    className,
                    methodName));
        }
    }
}
