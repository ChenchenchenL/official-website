package com.company.officialwebsite.common.enums;

import java.util.Arrays;

/**
 * UserStatus：统一定义后台账号可登录状态，避免直接在代码中散落状态字符串。
 */
public enum UserStatus {

    ENABLED("ENABLED", "启用"),
    DISABLED("DISABLED", "禁用");

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public static UserStatus fromCode(String code) {
        return Arrays.stream(values())
                .filter(userStatus -> userStatus.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported user status: " + code));
    }
}
