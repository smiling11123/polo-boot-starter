package com.polo.boot.validation.util;

public class StringUtils {

    /**
     * 字符串脱敏
     * @param str 原字符串
     * @param start 保留前几位
     * @param end 保留后几位
     */
    public static String mask(String str, int start, int end) {
        if (str == null || str.length() <= start + end) {
            return str;
        }
        return str.substring(0, start) +
                "*".repeat(str.length() - start - end) +
                str.substring(str.length() - end);
    }

    /**
     * 缩写字符串
     * @param str 原字符串
     * @param maxWidth 最大长度
     */
    public static String abbreviate(String str, int maxWidth) {
        if (str == null) return null;
        if (str.length() <= maxWidth) return str;
        return str.substring(0, maxWidth - 3) + "...";
    }
}