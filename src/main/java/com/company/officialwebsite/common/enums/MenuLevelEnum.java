package com.company.officialwebsite.common.enums;

/**
 * MenuLevelEnum：定义导航菜单允许的固定层级。
 */
public enum MenuLevelEnum {

    LEVEL_1((byte) 1),
    LEVEL_2((byte) 2);

    private final byte code;

    MenuLevelEnum(byte code) {
        this.code = code;
    }

    public byte getCode() {
        return code;
    }

    public static MenuLevelEnum fromCode(Byte code) {
        if (code == null) {
            throw new IllegalArgumentException("menu level code must not be null");
        }
        for (MenuLevelEnum value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("unsupported menu level code: " + code);
    }
}
