package io.github.ivencn.infra.web;

import java.util.HashSet;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * iven-starter-web 配置属性。前缀：iven.web。
 */
@Data
@ConfigurationProperties(prefix = "iven.web")
public class WebInfraProperties {

    private final Logging logging = new Logging();

    /**
     * 请求日志拦截器配置。
     */
    @Data
    public static class Logging {
        /**
         * 是否注册请求日志拦截器
         */
        private boolean enabled = true;

        /**
         * 不记录日志的路径（Ant 风格），如 /api/v1/health、/actuator/**
         */
        private Set<String> excludePaths = new HashSet<>();
    }
}
