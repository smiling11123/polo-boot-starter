package com.polo.boot.api.doc.service;

import com.polo.boot.api.doc.properties.OpenApiProperties;
import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class SwaggerUiGroupsConfigurer implements SmartInitializingSingleton {
    private final SwaggerUiConfigProperties swaggerUiConfigProperties;
    private final OpenApiProperties openApiProperties;

    public SwaggerUiGroupsConfigurer(SwaggerUiConfigProperties swaggerUiConfigProperties,
                                     OpenApiProperties openApiProperties) {
        this.swaggerUiConfigProperties = swaggerUiConfigProperties;
        this.openApiProperties = openApiProperties;
    }

    @Override
    public void afterSingletonsInstantiated() {
        if (!openApiProperties.isEnabled()) {
            return;
        }
        if (swaggerUiConfigProperties.getUrls() != null && !swaggerUiConfigProperties.getUrls().isEmpty()) {
            return;
        }

        List<OpenApiGroupResolver.GroupDefinition> groups = OpenApiGroupResolver.resolve(openApiProperties);
        if (groups.isEmpty()) {
            return;
        }

        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new LinkedHashSet<>();
        for (OpenApiGroupResolver.GroupDefinition group : groups) {
            if (!StringUtils.hasText(group.group())) {
                continue;
            }
            String name = StringUtils.hasText(group.displayName()) ? group.displayName() : group.group();
            AbstractSwaggerUiConfigProperties.SwaggerUrl swaggerUrl = new AbstractSwaggerUiConfigProperties.SwaggerUrl();
            swaggerUrl.setName(name);
            swaggerUrl.setDisplayName(name);
            swaggerUrl.setUrl("/v3/api-docs/" + group.group());
            urls.add(swaggerUrl);
        }

        if (urls.isEmpty()) {
            return;
        }

        swaggerUiConfigProperties.setUrls(urls);
        swaggerUiConfigProperties.setDisableSwaggerDefaultUrl(true);
        if (!StringUtils.hasText(swaggerUiConfigProperties.getUrlsPrimaryName())) {
            swaggerUiConfigProperties.setUrlsPrimaryName(urls.iterator().next().getName());
        }
    }
}
