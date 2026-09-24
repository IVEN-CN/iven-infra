package io.github.ivencn.infra.security.ratelimit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.RateLimiter;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * 匿名上传限流器。
 *
 * <p>
 * 基于 Guava RateLimiter，按客户端 IP 限流，默认每 5 秒允许 1 次请求。
 * </p>
 */
public class AnonymousUploadRateLimiter {

    private final LoadingCache<String, RateLimiter> limiters = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build(new CacheLoader<>() {
                @Override
                public RateLimiter load(String key) {
                    return RateLimiter.create(0.2); // 每 5 秒 1 个请求
                }
            });

    /**
     * 尝试获取许可。
     *
     * @param clientIp
     *            客户端 IP
     * @return true 表示获得许可
     */
    public boolean tryAcquire(String clientIp) {
        if (clientIp == null || clientIp.isBlank()) {
            return false;
        }
        try {
            return limiters.get(clientIp).tryAcquire();
        } catch (ExecutionException e) {
            return false;
        }
    }
}
