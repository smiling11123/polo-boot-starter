package com.polo.boot.api.doc.service;

import com.polo.boot.api.doc.properties.OpenApiProperties;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GroupedOpenApiBeanRegistrar implements BeanDefinitionRegistryPostProcessor, EnvironmentAware {
    private Environment environment;

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        OpenApiProperties properties = Binder.get(environment)
                .bind("polo.api-doc", Bindable.of(OpenApiProperties.class))
                .orElseGet(OpenApiProperties::new);

        Set<String> registeredBeanNames = new LinkedHashSet<>();
        for (OpenApiGroupResolver.GroupDefinition groupDefinition : OpenApiGroupResolver.resolve(properties)) {
            if (!StringUtils.hasText(groupDefinition.group())) {
                continue;
            }
            String beanName = "groupedOpenApi_" + sanitizeBeanName(groupDefinition.group());
            if (registry.containsBeanDefinition(beanName) || !registeredBeanNames.add(beanName)) {
                continue;
            }
            BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
                    GroupedOpenApi.class,
                    () -> buildGroupedOpenApi(groupDefinition)
            );
            registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
    }

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    private GroupedOpenApi buildGroupedOpenApi(OpenApiGroupResolver.GroupDefinition groupDefinition) {
        GroupedOpenApi.Builder builder = GroupedOpenApi.builder()
                .group(groupDefinition.group());

        if (StringUtils.hasText(groupDefinition.displayName())) {
            builder.displayName(groupDefinition.displayName());
        }
        if (OpenApiGroupResolver.hasAny(groupDefinition.pathsToMatch())) {
            builder.pathsToMatch(groupDefinition.pathsToMatch());
        }
        if (OpenApiGroupResolver.hasAny(groupDefinition.pathsToExclude())) {
            builder.pathsToExclude(groupDefinition.pathsToExclude());
        }
        if (OpenApiGroupResolver.hasAny(groupDefinition.packagesToScan())) {
            builder.packagesToScan(groupDefinition.packagesToScan());
        }
        return builder.build();
    }

    private String sanitizeBeanName(String group) {
        return group.replaceAll("[^a-zA-Z0-9_\\-]", "_");
    }
}
