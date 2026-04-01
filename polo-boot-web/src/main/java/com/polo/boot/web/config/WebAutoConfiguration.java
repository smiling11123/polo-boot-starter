package com.polo.boot.web.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polo.boot.web.aspect.OperationLogAspect;
import com.polo.boot.web.handler.GlobalExceptionHandler;
import com.polo.boot.web.model.DefaultRecorder;
import com.polo.boot.web.model.LogRecorder;
import com.polo.boot.web.properties.WebProperties;
import com.polo.boot.web.spi.OperatorContextProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

import java.util.concurrent.Executor;

@AutoConfiguration
@EnableConfigurationProperties(WebProperties.class)
@ConditionalOnProperty(prefix = "polo.web", name = "enabled", havingValue = "true", matchIfMissing = true)
public class WebAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.web", name = "exception-handler-enabled", havingValue = "true", matchIfMissing = true)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.web", name = "operation-log-enabled", havingValue = "true", matchIfMissing = true)
    public OperationLogAspect operationLogAspect(LogRecorder logRecorder,
                                                 ObjectProvider<OperatorContextProvider> operatorContextProvider,
                                                 @Qualifier("taskExecutor") ObjectProvider<Executor> taskExecutorProvider) {
        return new OperationLogAspect(
                logRecorder,
                operatorContextProvider.getIfAvailable(OperatorContextProvider::anonymous),
                taskExecutorProvider.getIfAvailable()
        );
    }

    @Bean
    @ConditionalOnMissingBean(LogRecorder.class)
    @ConditionalOnProperty(prefix = "polo.web", name = "operation-log-enabled", havingValue = "true", matchIfMissing = true)
    public LogRecorder defaultRecorder(ObjectMapper objectMapper) {
        return new DefaultRecorder(objectMapper);
    }
    
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "polo.web", name = "operation-log-enabled", havingValue = "true", matchIfMissing = true)
    public com.polo.boot.web.interceptor.OperationLogInterceptor operationLogInterceptor(
            LogRecorder logRecorder, ObjectProvider<OperatorContextProvider> operatorContextProvider) {
        return new com.polo.boot.web.interceptor.OperationLogInterceptor(
                logRecorder, operatorContextProvider.getIfAvailable(OperatorContextProvider::anonymous));
    }

    @Bean
    public org.springframework.web.servlet.config.annotation.WebMvcConfigurer operationLogWebMvcConfigurer(
            ObjectProvider<com.polo.boot.web.interceptor.OperationLogInterceptor> interceptorProvider) {
        return new org.springframework.web.servlet.config.annotation.WebMvcConfigurer() {
            @Override
            public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
                com.polo.boot.web.interceptor.OperationLogInterceptor interceptor = interceptorProvider.getIfAvailable();
                if (interceptor != null) {
                    registry.addInterceptor(interceptor)
                            .addPathPatterns("/**")
                            // 最高优先级，确保它无论如何都是第一个执行preHandle，最后一个执行afterCompletion
                            .order(org.springframework.core.Ordered.HIGHEST_PRECEDENCE);
                }
            }
        };
    }
}
