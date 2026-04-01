package com.polo.boot.context.trans.config;

import com.polo.boot.context.trans.decorator.ComprehensiveTaskDecorator;
import com.polo.boot.context.trans.properties.ContextTransProperties;
import com.polo.boot.storage.properties.StorageProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@AutoConfiguration
@ConditionalOnClass({TaskDecorator.class, ThreadPoolTaskExecutor.class})
@EnableConfigurationProperties(ContextTransProperties.class)
@ConditionalOnProperty(prefix = "polo.context-trans", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AsyncConfig {

    @Bean
    @ConditionalOnMissingBean(TaskDecorator.class)
    public TaskDecorator comprehensiveTaskDecorator(ContextTransProperties properties,
                                                    ObjectProvider<StorageProperties> storagePropertiesProvider) {
        StorageProperties storageProperties = storagePropertiesProvider.getIfAvailable();
        boolean uploadContextEnabled = storageProperties != null && storageProperties.isEnabled();
        return new ComprehensiveTaskDecorator(properties, uploadContextEnabled);
    }

    @Bean("taskExecutor")
    @ConditionalOnMissingBean(name = "taskExecutor")
    @ConditionalOnProperty(prefix = "polo.context-trans", name = "register-default-executors", havingValue = "true", matchIfMissing = true)
    public Executor taskExecutor(TaskDecorator taskDecorator, ContextTransProperties properties) {
        return buildExecutor(taskDecorator, properties.getExecutor());
    }

    @Bean("ioExecutor")
    @ConditionalOnMissingBean(name = "ioExecutor")
    @ConditionalOnProperty(prefix = "polo.context-trans", name = "register-default-executors", havingValue = "true", matchIfMissing = true)
    public Executor ioExecutor(TaskDecorator taskDecorator, ContextTransProperties properties) {
        ContextTransProperties.Executor config = properties.getIoExecutor();
        if (config.getThreadNamePrefix() == null || config.getThreadNamePrefix().isBlank()) {
            config.setThreadNamePrefix("polo-io-");
        }
        return buildExecutor(taskDecorator, config);
    }

    private Executor buildExecutor(TaskDecorator taskDecorator, ContextTransProperties.Executor config) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(config.getCorePoolSize());
        executor.setMaxPoolSize(config.getMaxPoolSize());
        executor.setQueueCapacity(config.getQueueCapacity());
        executor.setKeepAliveSeconds(config.getKeepAliveSeconds());
        executor.setThreadNamePrefix(config.getThreadNamePrefix());
        executor.setTaskDecorator(taskDecorator);
        executor.setWaitForTasksToCompleteOnShutdown(config.isWaitForTasksToCompleteOnShutdown());
        executor.setAwaitTerminationSeconds(config.getAwaitTerminationSeconds());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
