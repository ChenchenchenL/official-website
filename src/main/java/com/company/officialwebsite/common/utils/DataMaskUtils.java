package com.company.officialwebsite.common.utils;

/**
 * DataMaskUtils：统一提供邮箱、电话等敏感字段的脱敏规则。
 */
public final class DataMaskUtils {

    private DataMaskUtils() {}

    /**
     * 邮箱脱敏：保留首字符 + *** + @ + 域名。
     */
    public static String maskEmail(String email) {
        if (!hasText(email)) {
            return "";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * 电话脱敏：保留前 3 位 + *** + 后 2 位。
     */
    public static String maskPhone(String phone) {
        if (!hasText(phone)) {
            return "";
        }
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() <= 5) {
            return "***";
        }
        int total = phone.length();
        int keepStart = 3;
        int keepEnd = 2;
        int maskCount = Math.max(1, total - keepStart - keepEnd);
        return phone.substring(0, keepStart) + "*".repeat(maskCount) + phone.substring(total - keepEnd);
    }

    /**
     * 需求描述摘要：截取指定长度并追加省略号。
     */
    public static String previewText(String text, int maxLength) {
        if (!hasText(text)) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
