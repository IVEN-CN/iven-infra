package io.github.ivencn.infra.rbac.spi;

import io.github.ivencn.infra.rbac.role.Role;

/**
 * 超级管理员策略 SPI。
 *
 * <p>
 * RBAC 切面在校验权限集合前询问本策略是否放行（超级管理员短路）。
 * 应用实现自身的角色层级规则；默认无超级管理员。
 * </p>
 */
public interface SuperAdminPolicy {

    SuperAdminPolicy NONE = role -> false;

    /**
     * 判断角色是否为超级管理员。
     *
     * @param role
     *            当前角色（可能为 null）
     * @return true 表示超级管理员，直接放行
     */
    boolean isSuperAdmin(Role role);
}
