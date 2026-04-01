package com.polo.boot.validation.config;

import com.polo.boot.validation.properties.ValidationProperties;
import com.polo.boot.validation.provider.SensitiveWordProvider;
import com.polo.boot.validation.service.AiContentChecker;
import com.polo.boot.validation.service.ContentValidationRecorder;
import com.polo.boot.validation.service.ContentValidationRecordStore;
import com.polo.boot.validation.service.DefaultContentValidationRecorder;
import com.polo.boot.validation.service.SensitiveWordManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@AutoConfiguration
@ConditionalOnClass(RestTemplate.class)
@EnableConfigurationProperties(ValidationProperties.class)
@EnableScheduling
@ConditionalOnProperty(prefix = "polo.validation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ValidationAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SensitiveWordManager sensitiveWordManager(ValidationProperties properties,
                                                     ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                                     ObjectProvider<List<SensitiveWordProvider>> sensitiveWordProviders) {
        return new SensitiveWordManager(
                properties,
                redisTemplateProvider.getIfAvailable(),
                sensitiveWordProviders.getIfAvailable(List::of)
        );
    }

    @Bean
    @ConditionalOnMissingBean
    public AiContentChecker aiContentChecker(ValidationProperties properties,
                                             ObjectProvider<RestTemplate> restTemplateProvider) {
        return new AiContentChecker(restTemplateProvider.getIfAvailable(RestTemplate::new), properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ContentValidationRecorder contentValidationRecorder(
            ObjectProvider<ContentValidationRecordStore> recordStoreProvider) {
        return new DefaultContentValidationRecorder(recordStoreProvider);
    }


}
