package com.polo.boot.security.context;

import com.polo.boot.security.model.LoginUser;

import java.math.BigDecimal;
import java.math.BigInteger;

@SuppressWarnings("unchecked")
public final class SecurityAttributes {
    private SecurityAttributes() {
    }

    public static <T> T get(String key, Class<T> targetType) {
        return convert(UserContext.getAttribute(key), targetType);
    }

    public static Long tenantId() {
        return get(SecurityAttributeKeys.TENANT_ID, Long.class);
    }

    public static Long deptId() {
        return get(SecurityAttributeKeys.DEPT_ID, Long.class);
    }

    public static String dataScope() {
        return get(SecurityAttributeKeys.DATA_SCOPE, String.class);
    }

    public static Boolean isAdmin() {
        return get(SecurityAttributeKeys.IS_ADMIN, Boolean.class);
    }

    public static Long auditUserId() {
        Long auditUserId = get(SecurityAttributeKeys.AUDIT_USER_ID, Long.class);
        return auditUserId != null ? auditUserId : getCurrentUserId();
    }

    public static Long getCurrentUserId() {
        return UserContext.get() != null ? UserContext.get().getPrincipalId() : null;
    }

    public static void put(LoginUser loginUser, String key, Object value) {
        if (loginUser != null) {
            loginUser.putAttribute(key, value);
        }
    }

    public static <T> T convert(Object value, Class<T> targetType) {
        if (value == null || targetType == null) {
            return null;
        }
        Class<?> actualTargetType = wrapPrimitiveType(targetType);
        if (actualTargetType.isInstance(value)) {
            return (T) actualTargetType.cast(value);
        }
        if (actualTargetType == String.class) {
            return (T) String.valueOf(value);
        }
        if (value instanceof Number number) {
            if (actualTargetType == Long.class) {
                return (T) Long.valueOf(number.longValue());
            }
            if (actualTargetType == Integer.class) {
                return (T) Integer.valueOf(number.intValue());
            }
            if (actualTargetType == Double.class) {
                return (T) Double.valueOf(number.doubleValue());
            }
            if (actualTargetType == Float.class) {
                return (T) Float.valueOf(number.floatValue());
            }
            if (actualTargetType == Short.class) {
                return (T) Short.valueOf(number.shortValue());
            }
            if (actualTargetType == Byte.class) {
                return (T) Byte.valueOf(number.byteValue());
            }
            if (actualTargetType == BigDecimal.class) {
                return (T) BigDecimal.valueOf(number.doubleValue());
            }
            if (actualTargetType == BigInteger.class) {
                return (T) BigInteger.valueOf(number.longValue());
            }
        }
        if (actualTargetType == Boolean.class) {
            if (value instanceof Boolean booleanValue) {
                return (T) booleanValue;
            }
            if (value instanceof String stringValue) {
                return (T) Boolean.valueOf(Boolean.parseBoolean(stringValue));
            }
            if (value instanceof Number number) {
                return (T) Boolean.valueOf(number.intValue() != 0);
            }
        }
        if (actualTargetType.isEnum() && value instanceof String stringValue) {
            return (T) Enum.valueOf((Class<? extends Enum>) actualTargetType.asSubclass(Enum.class), stringValue);
        }
        return null;
    }

    private static Class<?> wrapPrimitiveType(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }
}
