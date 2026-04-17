package com.polo.boot.security.context;

import com.polo.boot.security.annotation.SecurityAttributesField;
import com.polo.boot.security.annotation.SecurityPrincipalField;
import com.polo.boot.security.annotation.SecurityPrincipalType;
import com.polo.boot.security.model.UserPrincipal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SecurityPrincipalSupport {
    private static final Map<Class<?>, PrincipalMetadata> CACHE = new ConcurrentHashMap<>();

    private SecurityPrincipalSupport() {
    }

    public static boolean supportsType(Class<?> targetType) {
        if (targetType == null) {
            return false;
        }
        if (UserPrincipal.class.isAssignableFrom(targetType)) {
            return true;
        }
        return metadata(targetType).principalFields().containsKey(SecurityPrincipalType.PRINCIPAL_ID);
    }

    public static UserPrincipal adapt(Object principalSource) {
        if (principalSource == null) {
            throw new IllegalArgumentException("登录用户对象不能为空");
        }
        if (principalSource instanceof UserPrincipal userPrincipal) {
            return userPrincipal;
        }

        PrincipalMetadata metadata = metadata(principalSource.getClass());
        if (!metadata.principalFields().containsKey(SecurityPrincipalType.PRINCIPAL_ID)) {
            throw new IllegalArgumentException("""
                    登录用户对象缺少主身份标记，至少需要一个字段加上 @SecurityPrincipalField(type = SecurityPrincipalType.PRINCIPAL_ID)
                    """.trim());
        }

        Long principalId = readRequiredPrincipalId(principalSource, metadata);
        String principalName = readPrincipalValue(principalSource, metadata, SecurityPrincipalType.PRINCIPAL_NAME);
        String principalRole = readPrincipalValue(principalSource, metadata, SecurityPrincipalType.PRINCIPAL_ROLE);

        Map<String, Object> attributes = new LinkedHashMap<>(SecurityPrincipalAttributesResolver.resolve(principalSource));
        mergeAttributes(attributes, readAttributeMap(principalSource, metadata));
        return new AdaptedUserPrincipal(principalId, principalName, principalRole, attributes);
    }

    public static <T> T convert(UserPrincipal source, Class<T> targetType) {
        if (source == null) {
            return null;
        }
        if (targetType == null) {
            throw new IllegalArgumentException("目标用户类型不能为空");
        }
        if (targetType.isInstance(source)) {
            return targetType.cast(source);
        }
        if (!supportsType(targetType)) {
            throw new IllegalArgumentException("`@CurrentUser` 目标类型不受支持: " + targetType.getName()
                    + "，请实现 UserPrincipal，或在字段上添加 @SecurityPrincipalField");
        }

        T target = instantiate(targetType);
        PrincipalMetadata metadata = metadata(targetType);

        writePrincipalField(target, metadata, SecurityPrincipalType.PRINCIPAL_ID, source.getPrincipalId());
        writePrincipalField(target, metadata, SecurityPrincipalType.PRINCIPAL_NAME, source.getPrincipalName());
        writePrincipalField(target, metadata, SecurityPrincipalType.PRINCIPAL_ROLE, source.getPrincipalRole());

        for (SecurityAnnotatedAttributeResolver.AnnotatedField annotatedField : SecurityAnnotatedAttributeResolver.getAnnotatedFields(targetType)) {
            Object attributeValue = source.getAttribute(annotatedField.attributeKey());
            writeField(target, annotatedField.field(), attributeValue);
        }

        if (metadata.attributesField() != null) {
            writeField(target, metadata.attributesField(), copyAttributes(source.getAttributes()));
        }
        return target;
    }

    private static PrincipalMetadata metadata(Class<?> type) {
        return CACHE.computeIfAbsent(type, SecurityPrincipalSupport::scanMetadata);
    }

    private static PrincipalMetadata scanMetadata(Class<?> sourceType) {
        Map<SecurityPrincipalType, Field> principalFields = new LinkedHashMap<>();
        Field attributesField = null;

        for (Class<?> current = sourceType; current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                SecurityPrincipalField principalField = field.getAnnotation(SecurityPrincipalField.class);
                if (principalField != null && !principalFields.containsKey(principalField.type())) {
                    field.setAccessible(true);
                    principalFields.put(principalField.type(), field);
                }
                if (attributesField == null && field.isAnnotationPresent(SecurityAttributesField.class)) {
                    field.setAccessible(true);
                    attributesField = field;
                }
            }
        }

        return new PrincipalMetadata(Map.copyOf(principalFields), attributesField);
    }

    private static Long readRequiredPrincipalId(Object source, PrincipalMetadata metadata) {
        Object rawValue = readFieldValue(source, metadata.principalFields().get(SecurityPrincipalType.PRINCIPAL_ID));
        Long principalId = SecurityAttributes.convert(rawValue, Long.class);
        if (principalId == null) {
            throw new IllegalArgumentException("登录用户对象中的 PRINCIPAL_ID 字段不能为空，且必须能转换为 Long");
        }
        return principalId;
    }

    private static String readPrincipalValue(Object source, PrincipalMetadata metadata, SecurityPrincipalType type) {
        Object rawValue = readFieldValue(source, metadata.principalFields().get(type));
        return SecurityAttributes.convert(rawValue, String.class);
    }

    private static Object readFieldValue(Object source, Field field) {
        if (field == null || source == null) {
            return null;
        }
        try {
            return field.get(source);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("读取登录用户字段失败: " + field.getName(), e);
        }
    }

    private static Map<String, Object> readAttributeMap(Object source, PrincipalMetadata metadata) {
        Field field = metadata.attributesField();
        if (field == null) {
            return Map.of();
        }
        Object rawValue = readFieldValue(source, field);
        if (!(rawValue instanceof Map<?, ?> rawMap) || rawMap.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> attributes = new LinkedHashMap<>();
        rawMap.forEach((key, value) -> {
            if (key != null && value != null) {
                attributes.put(String.valueOf(key), value);
            }
        });
        return attributes;
    }

    private static <T> T instantiate(Class<T> targetType) {
        try {
            Constructor<T> constructor = targetType.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("`@CurrentUser` 目标类型必须提供无参构造器: " + targetType.getName(), e);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("实例化当前用户对象失败: " + targetType.getName(), e);
        }
    }

    private static <T> void writePrincipalField(T target,
                                                PrincipalMetadata metadata,
                                                SecurityPrincipalType type,
                                                Object value) {
        Field field = metadata.principalFields().get(type);
        if (field != null) {
            writeField(target, field, value);
        }
    }

    private static <T> void writeField(T target, Field field, Object value) {
        if (field == null) {
            return;
        }
        Object convertedValue = SecurityAttributes.convert(value, field.getType());
        if (convertedValue == null && value != null && !field.getType().isInstance(value)) {
            convertedValue = value;
        }
        if (convertedValue == null && field.getType().isPrimitive()) {
            return;
        }
        try {
            field.set(target, convertedValue);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("写入当前用户字段失败: " + field.getName(), e);
        }
    }

    private static void mergeAttributes(Map<String, Object> target, Map<String, Object> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        source.forEach((key, value) -> {
            if (key != null && !key.isBlank() && value != null) {
                target.put(key, value);
            }
        });
    }

    private static Map<String, Object> copyAttributes(Map<String, Object> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return new LinkedHashMap<>();
        }
        return new LinkedHashMap<>(attributes);
    }

    private record PrincipalMetadata(Map<SecurityPrincipalType, Field> principalFields, Field attributesField) {
    }

    private static final class AdaptedUserPrincipal implements UserPrincipal {
        private final Long principalId;
        private final String principalName;
        private final String principalRole;
        private final Map<String, Object> attributes;

        private AdaptedUserPrincipal(Long principalId,
                                     String principalName,
                                     String principalRole,
                                     Map<String, Object> attributes) {
            this.principalId = principalId;
            this.principalName = principalName;
            this.principalRole = principalRole;
            this.attributes = attributes == null || attributes.isEmpty()
                    ? Map.of()
                    : Map.copyOf(attributes);
        }

        @Override
        public String getPrincipalName() {
            return principalName;
        }

        @Override
        public Long getPrincipalId() {
            return principalId;
        }

        @Override
        public String getPrincipalRole() {
            return principalRole;
        }

        @Override
        public Map<String, Object> getAttributes() {
            return attributes;
        }
    }
}
