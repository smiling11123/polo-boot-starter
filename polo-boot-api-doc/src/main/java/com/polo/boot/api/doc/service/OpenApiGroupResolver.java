package com.polo.boot.api.doc.service;

import com.polo.boot.api.doc.properties.OpenApiProperties;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class OpenApiGroupResolver {
    private OpenApiGroupResolver() {
    }

    public static List<GroupDefinition> resolve(OpenApiProperties properties) {
        List<GroupDefinition> groups = new ArrayList<>();
        if (properties.isCreateDefaultGroup()) {
            groups.add(new GroupDefinition(
                    properties.getDefaultGroupName(),
                    properties.getDefaultGroupDisplayName(),
                    properties.getPathsToMatch(),
                    properties.getPathsToExclude(),
                    properties.getPackagesToScan()
            ));
        }

        if (hasAny(properties.getPublicPath()) || hasAny(properties.getPublicPackage())) {
            groups.add(new GroupDefinition(
                    "public",
                    "Public APIs",
                    properties.getPublicPath(),
                    new String[0],
                    properties.getPublicPackage()
            ));
        }

        if (hasAny(properties.getAdminPath()) || hasAny(properties.getAdminPackage())) {
            groups.add(new GroupDefinition(
                    "admin",
                    "Admin APIs",
                    properties.getAdminPath(),
                    new String[0],
                    properties.getAdminPackage()
            ));
        }

        for (OpenApiProperties.GroupConfig groupConfig : properties.getGroups()) {
            if (groupConfig == null || !groupConfig.isEnabled() || !StringUtils.hasText(groupConfig.getName())) {
                continue;
            }
            groups.add(new GroupDefinition(
                    groupConfig.getName(),
                    StringUtils.hasText(groupConfig.getDisplayName()) ? groupConfig.getDisplayName() : groupConfig.getDescription(),
                    groupConfig.getPath(),
                    groupConfig.getExcludePath(),
                    groupConfig.getPackages()
            ));
        }
        return groups;
    }

    public static boolean hasAny(String[] values) {
        return values != null && values.length > 0;
    }

    public record GroupDefinition(String group,
                                  String displayName,
                                  String[] pathsToMatch,
                                  String[] pathsToExclude,
                                  String[] packagesToScan) {
    }
}
