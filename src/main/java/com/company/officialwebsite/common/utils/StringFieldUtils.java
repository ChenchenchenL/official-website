package com.company.officialwebsite.common.utils;

/**
 * StringFieldUtils：统一处理业务字符串字段的空白标准化与默认值回填。
 */
public final class StringFieldUtils {

    private StringFieldUtils() {
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    public static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    public static String defaultString(String value) {
        return value == null ? "" : value;
    }

    public static boolean isBlank(String value) {
        return trimToNull(value) == null;
    }
}
