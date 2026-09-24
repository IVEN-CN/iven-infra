package io.github.ivencn.infra.security.cookie;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Cookie 配置属性。前缀：iven.security.cookie。
 */
@Data
@ConfigurationProperties(prefix = "iven.security.cookie")
public class CookieProperties {

    /**
     * Cookie Domain，跨子域共享时设置为 .example.com，开发环境通常不设置
     */
    private String domain;

    /**
     * 是否启用 Secure 属性，生产环境应为 true（需要 HTTPS）
     */
    private boolean secure = false;

    /**
     * SameSite 属性，可选值: Strict, Lax, None
     */
    private String sameSite = "Lax";

    /**
     * JWT Cookie 名称
     */
    private String authCookieName = "auth_token";

    /**
     * CSRF Token Cookie 名称
     */
    private String csrfCookieName = "csrf_token";

    /**
     * Cookie 有效期（秒），默认 12 小时
     */
    private int maxAge = 43200;

    /**
     * Cookie 路径
     */
    private String path = "/";
}
