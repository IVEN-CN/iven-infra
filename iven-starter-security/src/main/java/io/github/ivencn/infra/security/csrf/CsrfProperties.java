package io.github.ivencn.infra.security.csrf;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

import lombok.Data;

/**
 * CSRF 防护配置属性。前缀：iven.security.csrf。
 */
@Data
@ConfigurationProperties(prefix = "iven.security.csrf")
public class CsrfProperties {

    /**
     * 请求头名称
     */
    private String headerName = "X-CSRF-Token";

    /**
     * 不需要 CSRF 验证的公开路径（Ant 风格），如登录、回调等接口
     */
    private Set<String> excludedPaths = new HashSet<>();
}
