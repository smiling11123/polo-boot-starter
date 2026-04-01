package com.polo.boot.security.context;

import com.polo.boot.security.annotation.SecurityAttributeField;
import com.polo.boot.security.annotation.SecurityAttributeType;
import com.polo.boot.security.model.UserPrincipal;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class SecurityAnnotatedAttributeResolver {
    private static final Map<Class<?>, List<AnnotatedField>> CACHE = new ConcurrentHashMap<>();

    private SecurityAnnotatedAttributeResolver() {
    }

    static Map<String, Object> resolve(UserPrincipal userPrincipal) {
        if (userPrincipal == null) {
            return Map.of();
        }

        List<AnnotatedField> annotatedFields = CACHE.computeIfAbsent(
                userPrincipal.getClass(),
                SecurityAnnotatedAttributeResolver::scanAnnotatedFields
        );
        if (annotatedFields.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        for (AnnotatedField annotatedField : annotatedFields) {
            try {
                Object value = annotatedField.field().get(userPrincipal);
                if (value != null) {
                    attributes.put(annotatedField.attributeKey(), value);
                }
            } catch (IllegalAccessException ignored) {
                // ignore invalid field access and keep resolving other attributes
            }
        }
        return attributes;
    }

    private static List<AnnotatedField> scanAnnotatedFields(Class<?> sourceType) {
        List<AnnotatedField> annotatedFields = new ArrayList<>();
        Set<SecurityAttributeType> seenTypes = EnumSet.noneOf(SecurityAttributeType.class);

        for (Class<?> current = sourceType; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                SecurityAttributeField annotation = field.getAnnotation(SecurityAttributeField.class);
                if (annotation == null || seenTypes.contains(annotation.type())) {
                    continue;
                }
                field.setAccessible(true);
                annotatedFields.add(new AnnotatedField(field, annotation.type().getAttributeKey()));
                seenTypes.add(annotation.type());
            }
        }
        return List.copyOf(annotatedFields);
    }

    private record AnnotatedField(Field field, String attributeKey) {
    }
}
