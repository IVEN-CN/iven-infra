package io.github.ivencn.infra.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * JWT 配置属性类。前缀：iven.security.jwt。
 */
@Data
@ConfigurationProperties(prefix = "iven.security.jwt")
public class JwtProperties {

    /**
     * JWT密钥，建议至少256位
     */
    private String secret = "yoursecretkeyheremustbeatleast256bitslongforsecurity";

    /**
     * Token过期时间（秒），默认12小时
     */
    private Long expiration = 43200L;

    /**
     * 载荷类，默认 {@link DefaultJwtPayload}；业务方可用全限定名指定自定义实现
     */
    private Class<? extends JwtClaims> payloadClass = DefaultJwtPayload.class;
}
