package com.polo.boot.security.support;

import com.polo.boot.security.annotation.RateLimit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class RedisRateLimiter {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<List> tokenBucketScript;
    private final DefaultRedisScript<List> slidingWindowScript;

    /**
     * 尝试获取许可
     */
    public boolean tryAcquire(String key, RateLimit limitAnnotation) {
        validateLimit(limitAnnotation);
        switch (limitAnnotation.algorithm()) {
            case TOKEN_BUCKET:
                return tryAcquireTokenBucket(key, limitAnnotation);
            case SLIDING_WINDOW:
                return tryAcquireSlidingWindow(key, limitAnnotation);
            case FIXED_WINDOW:
                return tryAcquireFixedWindow(key, limitAnnotation);
            default:
                return tryAcquireTokenBucket(key, limitAnnotation);
        }
    }

    /**
     * 令牌桶算法
     */
    private boolean tryAcquireTokenBucket(String key, RateLimit limit) {
        long now = System.currentTimeMillis() / 1000;

        List result = redisTemplate.execute(
                tokenBucketScript,
                Collections.singletonList("rate_limit:token:" + key),
                String.valueOf(limit.rate()),
                String.valueOf(limit.capacity()),
                String.valueOf(now),
                "1"  // 请求 1 个令牌
        );

        return isAllowed(result);
    }

    /**
     * 滑动窗口算法
     */
    private boolean tryAcquireSlidingWindow(String key, RateLimit limit) {
        long now = System.currentTimeMillis();
        long windowMs = (long) (limit.capacity() / limit.rate() * 1000);

        List result = redisTemplate.execute(
                slidingWindowScript,
                Collections.singletonList("rate_limit:window:" + key),
                String.valueOf(windowMs),
                String.valueOf(now),
                String.valueOf(limit.capacity())
        );

        return isAllowed(result);
    }

    /**
     * 固定窗口算法（简单实现）
     */
    private boolean tryAcquireFixedWindow(String key, RateLimit limit) {
        long now = System.currentTimeMillis() / 1000;
        long windowSeconds = Math.max(1L, (long) Math.ceil(limit.capacity() / limit.rate()));
        long window = now / windowSeconds;

        String redisKey = "rate_limit:fixed:" + key + ":" + window;

        Long count = redisTemplate.opsForValue().increment(redisKey);
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, Duration.ofSeconds(windowSeconds));
        }

        return count != null && count <= limit.capacity();
    }

    /**
     * 等待获取许可（带超时）
     */
    public boolean tryAcquireWithWait(String key, RateLimit limit, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            if (tryAcquire(key, limit)) {
                return true;
            }
            try {
                Thread.sleep(10);  // 短暂等待
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    private boolean isAllowed(List result) {
        if (result == null || result.isEmpty()) {
            return false;
        }
        Object first = result.get(0);
        return first instanceof Number number && number.longValue() == 1L;
    }

    private void validateLimit(RateLimit limitAnnotation) {
        if (limitAnnotation.rate() <= 0) {
            throw new IllegalArgumentException("@RateLimit.rate 必须大于 0");
        }
        if (limitAnnotation.capacity() <= 0) {
            throw new IllegalArgumentException("@RateLimit.capacity 必须大于 0");
        }
    }
}
