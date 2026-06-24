package com.company.officialwebsite.common.enums;

import java.util.Arrays;

/**
 * MediaAssetStatusEnum：统一约束媒体资源在上传、绑定、解绑和清理过程中的生命周期状态。
 */
public enum MediaAssetStatusEnum {

    TEMPORARY("TEMPORARY"),
    BOUND("BOUND"),
    UNBOUND("UNBOUND"),
    DELETED("DELETED");

    private final String code;

    MediaAssetStatusEnum(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static MediaAssetStatusEnum fromCode(String code) {
        return Arrays.stream(values())
                .filter(status -> status.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported media asset status: " + code));
    }
}
