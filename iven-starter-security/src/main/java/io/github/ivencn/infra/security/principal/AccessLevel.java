package io.github.ivencn.infra.security.principal;

/**
 * 访问级别枚举，定义接口的访问控制门槛。
 */
public enum AccessLevel {
    /**
     * 公开访问 - 无需认证和授权
     */
    PUBLIC,

    /**
     * 登录用户 - 需要有效的登录凭证
     */
    AUTHENTICATED,

    /**
     * 受保护 - 需要特定权限（由 RBAC 模块执行校验）
     */
    PROTECTED
}
