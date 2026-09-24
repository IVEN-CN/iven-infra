package io.github.ivencn.infra.security.oauth;

/**
 * OAuth state 值对象，绑定流程类型与可选关联用户。
 *
 * @param type
 *            OAuth 流程类型
 * @param userId
 *            绑定流程关联用户，登录流程为 null
 */
public record OAuthState(String type, Long userId) {
}
