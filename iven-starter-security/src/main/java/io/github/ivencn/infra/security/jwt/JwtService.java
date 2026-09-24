package io.github.ivencn.infra.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT 服务，提供 Token 的生成、解析和验证功能。
 *
 * <p>
 * 载荷类型由 {@link JwtProperties#getPayloadClass()} 决定，默认 {@link DefaultJwtPayload}；
 * 解析时按该类反序列化，自定义字段完整保留。
 * </p>
 */
@Slf4j
public class JwtService {

    private final JwtProperties jwtProperties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecretKey key;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        byte[] keyBytes = jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成 JWT Token。
     *
     * @param subjectId
     *            用户ID
     * @return 生成的 JWT 字符串
     */
    public String issue(Long subjectId) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + jwtProperties.getExpiration() * 1000);
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(subjectId.toString())
                .id(jti)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析 JWT Token。
     *
     * @param jwtString
     *            JWT 字符串
     * @return 载荷对象（按配置的载荷类实例化），解析失败返回 null
     */
    public JwtClaims parse(String jwtString) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(jwtString).getPayload();

            Map<String, Object> payload = new HashMap<>();
            payload.put("sub", Long.parseLong(claims.getSubject()));
            payload.put("jti", claims.getId());
            payload.put("iat", claims.getIssuedAt().getTime() / 1000);
            payload.put("exp", claims.getExpiration().getTime() / 1000);

            return objectMapper.convertValue(payload, jwtProperties.getPayloadClass());
        } catch (ExpiredJwtException e) {
            log.warn("JWT token has expired: {}", e.getMessage());
            return null;
        } catch (UnsupportedJwtException e) {
            log.warn("Unsupported JWT token: {}", e.getMessage());
            return null;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());
            return null;
        } catch (SignatureException e) {
            log.warn("Invalid JWT signature: {}", e.getMessage());
            return null;
        } catch (IllegalArgumentException e) {
            log.warn("JWT token is null or empty: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从 JWT 字符串中提取 JTI（JWT ID）。
     *
     * @param jwtString
     *            JWT 字符串
     * @return JTI 字符串，提取失败返回 null
     */
    public String getJti(String jwtString) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(jwtString).getPayload();
            return claims.getId();
        } catch (JwtException e) {
            log.warn("Failed to extract JTI from JWT: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 验证 JWT Token 是否有效。
     *
     * @param jwtString
     *            JWT 字符串
     * @return true 表示有效，false 表示无效
     */
    public boolean validateToken(String jwtString) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(jwtString);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
}
