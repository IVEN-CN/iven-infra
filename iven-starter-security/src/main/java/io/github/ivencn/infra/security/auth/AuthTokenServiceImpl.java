package io.github.ivencn.infra.security.auth;

import io.github.ivencn.infra.security.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 认证 Token 服务实现类，使用 Redis 存储 Token 白名单。
 */
@Slf4j
@RequiredArgsConstructor
public class AuthTokenServiceImpl implements AuthTokenService {

    private static final String TOKEN_KEY_PREFIX = "auth:token:";
    private static final String USER_TOKENS_KEY_PREFIX = "auth:user:tokens:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    @Override
    public void storeToken(String jti, Long userId) {
        String tokenKey = TOKEN_KEY_PREFIX + jti;
        String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;

        // 存储Token到白名单，设置过期时间
        redisTemplate.opsForValue().set(tokenKey, userId.toString(), jwtProperties.getExpiration(), TimeUnit.SECONDS);

        // 将Token添加到用户的Token集合中
        redisTemplate.opsForSet().add(userTokensKey, jti);
        // 设置用户Token集合的过期时间（比Token稍长，用于清理）
        redisTemplate.expire(userTokensKey, jwtProperties.getExpiration() + 3600, TimeUnit.SECONDS);

        // 吊销该用户的其他Token（单设备登录）
        revokeOtherTokens(userId, jti);

        log.debug("Token stored for user {}: {}", userId, jti);
    }

    @Override
    public Optional<Long> validateToken(String jti) {
        String tokenKey = TOKEN_KEY_PREFIX + jti;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);

        if (userIdStr == null) {
            log.debug("Token not found in whitelist: {}", jti);
            return Optional.empty();
        }

        try {
            Long userId = Long.parseLong(userIdStr);
            return Optional.of(userId);
        } catch (NumberFormatException e) {
            log.error("Invalid userId format in token whitelist: {}", userIdStr);
            return Optional.empty();
        }
    }

    @Override
    public boolean revokeToken(String jti) {
        String tokenKey = TOKEN_KEY_PREFIX + jti;
        String userIdStr = redisTemplate.opsForValue().get(tokenKey);

        if (userIdStr != null) {
            redisTemplate.delete(tokenKey);

            try {
                Long userId = Long.parseLong(userIdStr);
                String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;
                redisTemplate.opsForSet().remove(userTokensKey, jti);
            } catch (NumberFormatException e) {
                log.error("Invalid userId format when revoking token: {}", userIdStr);
            }

            log.debug("Token revoked: {}", jti);
            return true;
        }

        log.debug("Token not found when revoking: {}", jti);
        return false;
    }

    @Override
    public void revokeAllUserTokens(Long userId) {
        String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

        if (tokens != null && !tokens.isEmpty()) {
            for (String jti : tokens) {
                String tokenKey = TOKEN_KEY_PREFIX + jti;
                redisTemplate.delete(tokenKey);
            }
            redisTemplate.delete(userTokensKey);
            log.info("All tokens revoked for user: {}", userId);
        }
    }

    /**
     * 吊销用户的其他 Token（保留当前 Token）。
     *
     * @param userId
     *            用户ID
     * @param currentJti
     *            当前 Token 的 JTI
     */
    private void revokeOtherTokens(Long userId, String currentJti) {
        String userTokensKey = USER_TOKENS_KEY_PREFIX + userId;
        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

        if (tokens != null) {
            for (String jti : tokens) {
                if (!jti.equals(currentJti)) {
                    String tokenKey = TOKEN_KEY_PREFIX + jti;
                    redisTemplate.delete(tokenKey);
                    redisTemplate.opsForSet().remove(userTokensKey, jti);
                    log.debug("Revoked old token for user {}: {}", userId, jti);
                }
            }
        }
    }
}
