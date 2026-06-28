package com.company.officialwebsite.modules.lead.enums;

/**
 * LeadStatusEnum：定义线索跟进状态枚举及其对外展示名称。
 */
public enum LeadStatusEnum {

    UNHANDLED(0, "未处理"),
    PROCESSING(1, "处理中"),
    ARCHIVED(2, "已归档"),
    INVALID(3, "无效线索");

    private final int code;
    private final String label;

    LeadStatusEnum(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public int getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static LeadStatusEnum fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (LeadStatusEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        return null;
    }

    public static boolean isValid(Integer code) {
        return fromCode(code) != null;
    }

    public static String labelOf(Integer code) {
        LeadStatusEnum value = fromCode(code);
        return value == null ? "" : value.label;
    }
}
