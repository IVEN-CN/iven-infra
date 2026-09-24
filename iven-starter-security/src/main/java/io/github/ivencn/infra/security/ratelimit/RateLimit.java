package io.github.ivencn.infra.security.ratelimit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IP 限频注解。
 *
 * <p>
 * 标记在 Controller 方法上，基于 Redis 实现按 IP 的请求间隔限频。同一 IP 在不同接口上的限频独立计算。
 * </p>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 允许的最快访问间隔（秒）
     */
    int interval();
}
