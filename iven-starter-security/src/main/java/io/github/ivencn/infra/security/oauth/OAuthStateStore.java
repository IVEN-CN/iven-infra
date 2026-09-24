package io.github.ivencn.infra.security.oauth;

import java.time.Duration;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * OAuth state 存取组件，隔离 Redis 和 JSON 序列化细节。
 *
 * <p>
 * 发起 OAuth 授权时存储随机 state（TTL 10 分钟，绑定流程类型与可选用户），回调时一次性消费：
 * 不存在、过期或解析失败即拒绝该回调，防伪造与重放。
 * </p>
 */
@Slf4j
@RequiredArgsConstructor
public class OAuthStateStore {

    private static final long OAUTH_STATE_TTL_SECONDS = 600;
    private static final String OAUTH_STATE_KEY_PREFIX = "oauth:state:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 保存 OAuth state。
     *
     * @param state
     *            随机 state
     * @param type
     *            OAuth 流程类型
     * @param userId
     *            绑定流程关联用户，登录流程为 null
     */
    public void store(String state, String type, Long userId) {
        OAuthState oauthState = new OAuthState(type, userId);
        try {
            String stateJson = objectMapper.writeValueAsString(oauthState);
            redisTemplate.opsForValue()
                    .set(
                            OAUTH_STATE_KEY_PREFIX + state,
                            stateJson,
                            Duration.ofSeconds(OAUTH_STATE_TTL_SECONDS));
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize OAuth state", e);
        }
    }

    /**
     * 一次性读取并删除 OAuth state。
     *
     * @param state
     *            回调携带的 state
     * @return 解析出的 state；不存在或解析失败时返回 null
     */
    public OAuthState consume(String state) {
        String key = OAUTH_STATE_KEY_PREFIX + state;
        String stateJson = redisTemplate.opsForValue().getAndDelete(key);
        if (stateJson == null) {
            log.warn("OAuth state not found or expired: {}", state);
            return null;
        }
        try {
            return objectMapper.readValue(stateJson, OAuthState.class);
        } catch (Exception e) {
            log.error("Failed to deserialize OAuth state", e);
            return null;
        }
    }
}
