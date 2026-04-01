package com.polo.boot.mybatis.plus.service;

import java.math.BigDecimal;
import java.math.BigInteger;

final class ContextValueConverter {
    private ContextValueConverter() {
    }

    @SuppressWarnings("unchecked")
    static <T> T convert(Object value, Class<T> targetType) {
        if (value == null || targetType == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return (T) value;
        }

        Class<?> actualType = wrapPrimitive(targetType);
        if (actualType == String.class) {
            return (T) String.valueOf(value);
        }
        if (value instanceof Number number) {
            return convertNumber(number, actualType);
        }
        if (actualType == Boolean.class) {
            return (T) convertBoolean(value);
        }
        if (value instanceof CharSequence sequence) {
            String text = sequence.toString().trim();
            if (text.isEmpty()) {
                return null;
            }
            return convertText(text, actualType);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertNumber(Number number, Class<?> actualType) {
        if (actualType == Long.class) {
            return (T) Long.valueOf(number.longValue());
        }
        if (actualType == Integer.class) {
            return (T) Integer.valueOf(number.intValue());
        }
        if (actualType == Double.class) {
            return (T) Double.valueOf(number.doubleValue());
        }
        if (actualType == Float.class) {
            return (T) Float.valueOf(number.floatValue());
        }
        if (actualType == Short.class) {
            return (T) Short.valueOf(number.shortValue());
        }
        if (actualType == Byte.class) {
            return (T) Byte.valueOf(number.byteValue());
        }
        if (actualType == BigDecimal.class) {
            return (T) BigDecimal.valueOf(number.doubleValue());
        }
        if (actualType == BigInteger.class) {
            return (T) BigInteger.valueOf(number.longValue());
        }
        if (actualType == Boolean.class) {
            return (T) Boolean.valueOf(number.intValue() != 0);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T convertText(String text, Class<?> actualType) {
        try {
            if (actualType == Long.class) {
                return (T) Long.valueOf(text);
            }
            if (actualType == Integer.class) {
                return (T) Integer.valueOf(text);
            }
            if (actualType == Double.class) {
                return (T) Double.valueOf(text);
            }
            if (actualType == Float.class) {
                return (T) Float.valueOf(text);
            }
            if (actualType == Short.class) {
                return (T) Short.valueOf(text);
            }
            if (actualType == Byte.class) {
                return (T) Byte.valueOf(text);
            }
            if (actualType == BigDecimal.class) {
                return (T) new BigDecimal(text);
            }
            if (actualType == BigInteger.class) {
                return (T) new BigInteger(text);
            }
            if (actualType == Boolean.class) {
                return (T) convertBoolean(text);
            }
            if (actualType == String.class) {
                return (T) text;
            }
        } catch (NumberFormatException ignored) {
            return null;
        }
        return null;
    }

    private static Boolean convertBoolean(Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        if ("true".equalsIgnoreCase(text) || "1".equals(text) || "yes".equalsIgnoreCase(text)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(text) || "0".equals(text) || "no".equalsIgnoreCase(text)) {
            return Boolean.FALSE;
        }
        return null;
    }

    private static Class<?> wrapPrimitive(Class<?> type) {
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
        return type;
    }
}
