package com.polo.boot.mybatis.plus.service;

import com.polo.boot.mybatis.plus.annotation.AutoFillField;
import com.polo.boot.mybatis.plus.annotation.AutoFillType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

final class AutoFillFieldResolver {
    private static final Map<Class<?>, Map<AutoFillType, FieldBinding>> CACHE = new ConcurrentHashMap<>();

    private AutoFillFieldResolver() {
    }

    static Map<AutoFillType, FieldBinding> resolve(Class<?> entityType) {
        return CACHE.computeIfAbsent(entityType, AutoFillFieldResolver::scanBindings);
    }

    private static Map<AutoFillType, FieldBinding> scanBindings(Class<?> entityType) {
        Map<AutoFillType, FieldBinding> bindings = new EnumMap<>(AutoFillType.class);
        Set<AutoFillType> seenTypes = EnumSet.noneOf(AutoFillType.class);

        for (Class<?> current = entityType; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                AutoFillField annotation = field.getAnnotation(AutoFillField.class);
                if (annotation == null || seenTypes.contains(annotation.type())) {
                    continue;
                }
                bindings.put(annotation.type(), new FieldBinding(field.getName(), field.getType()));
                seenTypes.add(annotation.type());
            }
        }
        return Map.copyOf(bindings);
    }

    record FieldBinding(String propertyName, Class<?> fieldType) {
    }
}
