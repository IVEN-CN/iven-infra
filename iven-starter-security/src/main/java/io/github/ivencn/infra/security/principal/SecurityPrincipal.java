package io.github.ivencn.infra.security.principal;

import java.util.Collections;
import java.util.Set;

/**
 * 安全上下文主体，封装当前认证用户的核心安全属性（无业务依赖）。
 *
 * @param <R>
 *            角色类型，由 RBAC 模块的 {@code Role} 在应用装配时定型，security 层不感知
 * @param userId
 *            当前用户ID
 * @param role
 *            解析后的角色（可为 null）
 * @param permissions
 *            当前用户的权限值集合，不可变
 */
public record SecurityPrincipal<R>(Long userId, R role, Set<String> permissions) {

    /**
     * 构造安全上下文主体，权限集合会被包装为不可变集合。
     */
    public SecurityPrincipal {
        permissions = permissions != null ? Set.copyOf(permissions) : Collections.emptySet();
    }

    /**
     * 判断当前主体是否拥有指定权限。
     *
     * @param permission
     *            权限值
     * @return true 如果拥有该权限
     */
    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }
}
