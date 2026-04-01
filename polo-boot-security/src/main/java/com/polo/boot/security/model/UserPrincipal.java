package com.polo.boot.security.model;

import com.polo.boot.security.context.SecurityAttributeKeys;
import com.polo.boot.security.context.SecurityAttributes;
import com.polo.boot.security.context.SecurityPrincipalAttributesResolver;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public interface UserPrincipal {
    String getPrincipalName();
    Long getPrincipalId();
    String getPrincipalRole();

    default Map<String, Object> getAttributes() {
        return Map.of();
    }

    default Object getAttribute(String key) {
        return SecurityPrincipalAttributesResolver.getAttribute(this, key);
    }

    default <T> T getAttribute(String key, Class<T> type) {
        Object value = getAttribute(key);
        return SecurityAttributes.convert(value, type);
    }

    default boolean isAdmin() {
        Boolean adminFlag = getAttribute(SecurityAttributeKeys.IS_ADMIN, Boolean.class);
        if (adminFlag != null) {
            return adminFlag;
        }
        String role = getPrincipalRole();
        return role != null && ("admin".equalsIgnoreCase(role) || "super_admin".equalsIgnoreCase(role));
    }

    default Set<String> getPrincipalPermissions() {
        Object rawPermissions = getAttribute(SecurityAttributeKeys.PERMISSIONS);
        if (rawPermissions == null) {
            return Collections.emptySet();
        }

        LinkedHashSet<String> permissions = new LinkedHashSet<>();
        collectPermissions(rawPermissions, permissions);
        return permissions.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(permissions);
    }

    private static void collectPermissions(Object source, Set<String> target) {
        if (source == null) {
            return;
        }
        if (source instanceof String stringValue) {
            for (String permission : stringValue.split(",")) {
                if (permission != null) {
                    String trimmed = permission.trim();
                    if (!trimmed.isEmpty()) {
                        target.add(trimmed);
                    }
                }
            }
            return;
        }
        if (source instanceof Collection<?> collection) {
            for (Object item : collection) {
                collectPermissions(item, target);
            }
            return;
        }
        if (source.getClass().isArray()) {
            int length = Array.getLength(source);
            for (int i = 0; i < length; i++) {
                collectPermissions(Array.get(source, i), target);
            }
            return;
        }
        String permission = String.valueOf(source).trim();
        if (!permission.isEmpty()) {
            target.add(permission);
        }
    }
}
