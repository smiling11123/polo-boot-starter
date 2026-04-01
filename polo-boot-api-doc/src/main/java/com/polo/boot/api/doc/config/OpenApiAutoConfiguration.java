package com.polo.boot.api.doc.config;

import com.polo.boot.api.doc.properties.OpenApiProperties;
import com.polo.boot.api.doc.service.OpenApiResponseCustomizer;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@AutoConfiguration
@ConditionalOnClass({OpenAPI.class, GroupedOpenApi.class})
@EnableConfigurationProperties(OpenApiProperties.class)
@ConditionalOnProperty(prefix = "polo.api-doc", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public OpenAPI customOpenAPI(OpenApiProperties properties) {
        OpenAPI openAPI = new OpenAPI()
                .info(buildInfo(properties));

        List<Server> servers = buildServers(properties);
        if (!servers.isEmpty()) {
            openAPI.setServers(servers);
        }

        if (properties.isEnableSecurity()) {
            openAPI.components(new Components()
                    .addSecuritySchemes(
                            properties.getSecuritySchemeName(),
                            new SecurityScheme()
                                    .type(SecurityScheme.Type.HTTP)
                                    .scheme("bearer")
                                    .bearerFormat("JWT")
                                    .description(properties.getSecuritySchemeDescription())
                    ))
                    .addSecurityItem(new SecurityRequirement().addList(properties.getSecuritySchemeName()));
        }
        return openAPI;
    }

    @Bean
    @ConditionalOnMissingBean(OpenApiResponseCustomizer.class)
    public OperationCustomizer openApiResponseCustomizer(OpenApiProperties properties) {
        return new OpenApiResponseCustomizer(properties);
    }

    private Info buildInfo(OpenApiProperties properties) {
        Info info = new Info()
                .title(properties.getTitle())
                .description(properties.getDescription())
                .version(properties.getVersion());

        if (StringUtils.hasText(properties.getContactName())
                || StringUtils.hasText(properties.getContactEmail())
                || StringUtils.hasText(properties.getContactUrl())) {
            info.contact(new Contact()
                    .name(properties.getContactName())
                    .email(properties.getContactEmail())
                    .url(properties.getContactUrl()));
        }

        if (StringUtils.hasText(properties.getLicenseName())
                || StringUtils.hasText(properties.getLicenseUrl())) {
            info.license(new License()
                    .name(properties.getLicenseName())
                    .url(properties.getLicenseUrl()));
        }
        return info;
    }

    private List<Server> buildServers(OpenApiProperties properties) {
        List<Server> servers = new ArrayList<>();
        if (StringUtils.hasText(properties.getServerUrl())) {
            servers.add(new Server()
                    .url(properties.getServerUrl())
                    .description(properties.getServerDescription()));
        }
        return servers;
    }
}
