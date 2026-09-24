package io.github.ivencn.infra.security.principal;

import java.util.Collections;
import java.util.Set;

import org.springframework.lang.Nullable;

/**
 * 用户上下文，基于 ThreadLocal 存储当前请求的安全主体 {@link SecurityPrincipal}。
 *
 * <p>
 * 每个请求在 JWT 认证过滤器中构造 {@link SecurityPrincipal} 并写入本上下文，请求结束后必须调用
 * {@link #clear()} 清理，防止内存泄漏。
 * </p>
 */
public final class UserCTX {

    private static final ThreadLocal<SecurityPrincipal<?>> currentPrincipal = new ThreadLocal<>();

    private UserCTX() {
    }

    /**
     * 设置当前安全主体。
     *
     * @param principal
     *            安全主体
     */
    public static void setPrincipal(SecurityPrincipal<?> principal) {
        currentPrincipal.set(principal);
    }

    /**
     * 获取当前安全主体。
     *
     * @return 当前安全主体，未登录时返回 null
     */
    @Nullable
    public static SecurityPrincipal<?> getPrincipal() {
        return currentPrincipal.get();
    }

    /**
     * 获取当前用户ID。
     *
     * @return 用户ID，未登录时返回 null
     */
    @Nullable
    public static Long getCurrentUserId() {
        SecurityPrincipal principal = currentPrincipal.get();
        return principal != null ? principal.userId() : null;
    }

    /**
     * 获取当前用户角色。
     *
     * @return 角色（类型取决于应用装配时的定型），未登录或角色未知时返回 null
     */
    @Nullable
    public static Object getCurrentRole() {
        SecurityPrincipal<?> principal = currentPrincipal.get();
        return principal != null ? principal.role() : null;
    }

    /**
     * 获取当前用户权限集合。
     *
     * @return 权限集合，未登录时返回空集合
     */
    public static Set<String> getCurrentPermissions() {
        SecurityPrincipal principal = currentPrincipal.get();
        return principal != null ? principal.permissions() : Collections.emptySet();
    }

    /**
     * 检查是否已登录。
     *
     * @return true 如果已登录
     */
    public static boolean isAuthenticated() {
        return currentPrincipal.get() != null;
    }

    /**
     * 清除当前安全主体。必须在请求结束后调用，防止内存泄漏。
     */
    public static void clear() {
        currentPrincipal.remove();
    }
}
