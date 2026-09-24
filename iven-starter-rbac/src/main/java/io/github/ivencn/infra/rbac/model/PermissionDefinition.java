package io.github.ivencn.infra.rbac.model;

import io.github.ivencn.infra.security.principal.AccessLevel;

/**
 * 权限定义纯值对象，由扫描器产出并交付应用持久化。
 *
 * @param value
 *            权限唯一标识（resource:action）
 * @param name
 *            显示名称
 * @param url
 *            关联接口路径
 * @param method
 *            HTTP 方法
 * @param accessLevel
 *            访问级别
 */
public record PermissionDefinition(String value, String name, String url, String method,
        AccessLevel accessLevel) {
}
