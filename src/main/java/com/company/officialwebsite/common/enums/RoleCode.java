package com.company.officialwebsite.common.enums;

import com.company.officialwebsite.common.constants.SecurityConstants;
import java.util.Arrays;

/**
 * RoleCode：统一定义后台三角色模型及其 Spring Security 权限名。
 */
public enum RoleCode {

    ADMINISTRATOR("ADMINISTRATOR", "管理员"),
    CONTENT_EDITOR("CONTENT_EDITOR", "内容编辑员"),
    LEAD_FOLLOW_UP_OPERATOR("LEAD_FOLLOW_UP_OPERATOR", "线索跟进员");

    private final String code;
    private final String description;

    RoleCode(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 统一从角色码生成 GrantedAuthority，避免业务层散落字符串常量。
     */
    public String toAuthority() {
        return SecurityConstants.ROLE_PREFIX + code;
    }

    public static RoleCode fromCode(String code) {
        return Arrays.stream(values())
                .filter(roleCode -> roleCode.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported role code: " + code));
    }
}
