package io.github.ivencn.infra.security.jwt;

/**
 * JWT 载荷契约。
 *
 * <p>
 * 实现类的属性需与 JWT 声明对应：subjectId ↔ {@code sub}、jti ↔ {@code jti}、
 * issuedAt ↔ {@code iat}、expiration ↔ {@code exp}（可用 Jackson 注解显式声明）。
 * 业务方可通过 {@code iven.security.jwt.payload-class} 指定自定义实现，
 * 不指定时使用 {@link DefaultJwtPayload}。
 * </p>
 */
public interface JwtClaims {

    Long getSubjectId();

    String getJti();

    Long getIssuedAt();

    Long getExpiration();

    void setSubjectId(Long subjectId);

    void setJti(String jti);

    void setIssuedAt(Long issuedAt);

    void setExpiration(Long expiration);
}
