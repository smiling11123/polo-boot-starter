package com.polo.boot.cache.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polo.boot.cache.service.RedisService;
import com.polo.boot.cache.service.impl.RedisServiceImpl;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;

@AutoConfiguration
@ConditionalOnClass(StringRedisTemplate.class)
@EnableConfigurationProperties(CacheProperties.class)
@ConditionalOnProperty(prefix = "polo.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CacheAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.cache", name = "redis-service-enabled", havingValue = "true", matchIfMissing = true)
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.cache", name = "redis-service-enabled", havingValue = "true", matchIfMissing = true)
    public RedisService redisService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        return new RedisServiceImpl(stringRedisTemplate, objectMapper);
    }
}
