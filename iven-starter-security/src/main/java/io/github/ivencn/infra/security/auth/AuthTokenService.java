package io.github.ivencn.infra.security.auth;

import java.util.Optional;

/**
 * 认证 Token 服务接口，管理 JWT Token 的白名单存储、验证和吊销。
 */
public interface AuthTokenService {

    /**
     * 存储 Token 到白名单。
     *
     * @param jti
     *            JWT 唯一标识符
     * @param userId
     *            用户ID
     */
    void storeToken(String jti, Long userId);

    /**
     * 验证 Token 是否在白名单中。
     *
     * @param jti
     *            JWT 唯一标识符
     * @return Optional 包含用户ID，如果不存在则返回 empty
     */
    Optional<Long> validateToken(String jti);

    /**
     * 吊销 Token（从白名单中移除）。
     *
     * @param jti
     *            JWT 唯一标识符
     * @return 是否成功移除
     */
    boolean revokeToken(String jti);

    /**
     * 吊销用户的所有 Token。
     *
     * @param userId
     *            用户ID
     */
    void revokeAllUserTokens(Long userId);
}
