package io.github.ivencn.infra.security.jwt;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 默认 JWT 载荷数据类，用于存储 JWT Token 中的声明信息。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DefaultJwtPayload implements JwtClaims {

    /**
     * 用户ID（JWT subject）
     */
    @JsonProperty("sub")
    private Long subjectId;

    /**
     * JWT唯一标识符（JWT ID）
     */
    private String jti;

    /**
     * 签发时间（Unix时间戳，秒）
     */
    @JsonProperty("iat")
    private Long issuedAt;

    /**
     * 过期时间（Unix时间戳，秒）
     */
    @JsonProperty("exp")
    private Long expiration;
}
