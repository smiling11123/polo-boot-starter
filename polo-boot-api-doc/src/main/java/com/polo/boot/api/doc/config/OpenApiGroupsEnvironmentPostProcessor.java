package com.polo.boot.api.doc.config;

import com.polo.boot.api.doc.properties.OpenApiProperties;
import com.polo.boot.api.doc.service.OpenApiGroupResolver;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenApiGroupsEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {
    private static final String PROPERTY_SOURCE_NAME = "poloApiDocSpringDocGroups";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        boolean apiDocEnabled = environment.getProperty("polo.api-doc.enabled", Boolean.class, true);
        if (!apiDocEnabled) {
            return;
        }

        OpenApiProperties openApiProperties = Binder.get(environment)
                .bind("polo.api-doc", Bindable.of(OpenApiProperties.class))
                .orElseGet(OpenApiProperties::new);
        List<OpenApiGroupResolver.GroupDefinition> groups = OpenApiGroupResolver.resolve(openApiProperties);

        Map<String, Object> mappedProperties = new HashMap<>();
        if (!groups.isEmpty() && !environment.containsProperty("springdoc.api-docs.groups.enabled")) {
            mappedProperties.put("springdoc.api-docs.groups.enabled", "true");
        }
        if (!groups.isEmpty() && !environment.containsProperty("springdoc.group-configs[0].group")) {
            for (int i = 0; i < groups.size(); i++) {
                OpenApiGroupResolver.GroupDefinition group = groups.get(i);
                String prefix = "springdoc.group-configs[" + i + "]";
                mappedProperties.put(prefix + ".group", group.group());
                if (StringUtils.hasText(group.displayName())) {
                    mappedProperties.put(prefix + ".display-name", group.displayName());
                }
                putArrayProperties(mappedProperties, prefix + ".paths-to-match", group.pathsToMatch());
                putArrayProperties(mappedProperties, prefix + ".paths-to-exclude", group.pathsToExclude());
                putArrayProperties(mappedProperties, prefix + ".packages-to-scan", group.packagesToScan());
            }
        }
        if (!groups.isEmpty() && !hasExplicitSwaggerUiUrls(environment) && openApiProperties.isShowAllApisGroup()) {
            String allApisDisplayName = StringUtils.hasText(openApiProperties.getAllApisDisplayName())
                    ? openApiProperties.getAllApisDisplayName()
                    : "All APIs";
            mappedProperties.put("springdoc.swagger-ui.urls[0].name", allApisDisplayName);
            mappedProperties.put("springdoc.swagger-ui.urls[0].display-name", allApisDisplayName);
            mappedProperties.put("springdoc.swagger-ui.urls[0].url", "/v3/api-docs");
            if (!environment.containsProperty("springdoc.swagger-ui.urls-primary-name")) {
                mappedProperties.put("springdoc.swagger-ui.urls-primary-name", allApisDisplayName);
            }
            if (!environment.containsProperty("springdoc.swagger-ui.disable-swagger-default-url")) {
                mappedProperties.put("springdoc.swagger-ui.disable-swagger-default-url", "true");
            }
        }
        if (openApiProperties.isPersistAuthorization()
                && !hasExplicitPersistAuthorization(environment)) {
            mappedProperties.put("springdoc.swagger-ui.persist-authorization", "true");
        }

        if (!mappedProperties.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, mappedProperties));
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private void putArrayProperties(Map<String, Object> properties, String prefix, String[] values) {
        if (values == null) {
            return;
        }
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            if (StringUtils.hasText(value)) {
                properties.put(prefix + "[" + i + "]", value);
            }
        }
    }

    private boolean hasExplicitSwaggerUiUrls(ConfigurableEnvironment environment) {
        return environment.containsProperty("springdoc.swagger-ui.urls[0].url")
                || environment.containsProperty("springdoc.swagger-ui.urls[0].name")
                || environment.containsProperty("springdoc.swagger-ui.urls");
    }

    private boolean hasExplicitPersistAuthorization(ConfigurableEnvironment environment) {
        return environment.containsProperty("springdoc.swagger-ui.persist-authorization")
                || environment.containsProperty("springdoc.swagger-ui.persistAuthorization");
    }
}
