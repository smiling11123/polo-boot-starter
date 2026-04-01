package com.polo.boot.context.trans.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableAsync;

@AutoConfiguration
@EnableAsync
@ConditionalOnProperty(prefix = "polo.context-trans", name = "enable-async", havingValue = "true", matchIfMissing = true)
public class TaskDecoratorAutoConfiguration {
}
