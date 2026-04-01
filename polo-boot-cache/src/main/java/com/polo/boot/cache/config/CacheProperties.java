package com.polo.boot.cache.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "polo.cache")
public class CacheProperties {
    /**
     * 是否启用缓存模块自动装配。
     */
    private boolean enabled = true;

    /**
     * 是否注册 RedisService 及其依赖 Bean。
     */
    private boolean redisServiceEnabled = true;
}
