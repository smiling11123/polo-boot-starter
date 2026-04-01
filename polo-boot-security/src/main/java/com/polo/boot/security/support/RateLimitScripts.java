package com.polo.boot.security.support;

import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.List;

public class RateLimitScripts {

    /**
     * 令牌桶算法 Lua 脚本
     */
    public static final String TOKEN_BUCKET_SCRIPT = """
        local key = KEYS[1]
        local rate = tonumber(ARGV[1])
        local capacity = tonumber(ARGV[2])
        local now = tonumber(ARGV[3])
        local requested = tonumber(ARGV[4])
        
        -- 获取当前令牌数
        local bucket = redis.call('hmget', key, 'tokens', 'last_time')
        local tokens = tonumber(bucket[1]) or capacity
        local last_time = tonumber(bucket[2]) or now
        
        -- 计算新增令牌
        local delta = math.max(0, now - last_time)
        local new_tokens = math.min(capacity, tokens + delta * rate)
        
        -- 判断是否允许
        local allowed = new_tokens >= requested
        local remaining = new_tokens
        
        if allowed then
            new_tokens = new_tokens - requested
            remaining = new_tokens
        end
        
        -- 更新 Redis
        local ttl = math.ceil(capacity / rate * 2)
        redis.call('hmset', key, 'tokens', new_tokens, 'last_time', now)
        redis.call('expire', key, ttl)
        
        return {allowed and 1 or 0, remaining}
        """;

    /**
     * 滑动窗口算法 Lua 脚本
     */
    public static final String SLIDING_WINDOW_SCRIPT = """
        local key = KEYS[1]
        local window = tonumber(ARGV[1])
        local now = tonumber(ARGV[2])
        local threshold = tonumber(ARGV[3])
        
        -- 清理过期记录
        local min = now - window
        redis.call('zremrangebyscore', key, 0, min)
        
        -- 统计当前窗口内请求数
        local count = redis.call('zcard', key)
        
        -- 判断是否允许
        local allowed = count < threshold
        
        if allowed then
            redis.call('zadd', key, now, now .. ':' .. redis.call('incr', 'request_seq'))
            redis.call('expire', key, math.ceil(window / 1000))
        end
        
        return {allowed and 1 or 0, threshold - count - (allowed and 1 or 0)}
        """;

    @Bean
    public DefaultRedisScript<List> tokenBucketScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(TOKEN_BUCKET_SCRIPT);
        script.setResultType(List.class);
        return script;
    }

    @Bean
    public DefaultRedisScript<List> slidingWindowScript() {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptText(SLIDING_WINDOW_SCRIPT);
        script.setResultType(List.class);
        return script;
    }
}
