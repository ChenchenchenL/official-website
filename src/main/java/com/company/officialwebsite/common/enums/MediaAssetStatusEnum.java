package com.company.officialwebsite.common.enums;

import java.util.Arrays;

/**
 * MediaAssetStatusEnum：统一约束媒体资源在上传、绑定、解绑和清理过程中的生命周期状态。
 */
public enum MediaAssetStatusEnum {

    ACTIVE("ACTIVE"),
    DELETED("DELETED"),
    @Deprecated
    TEMPORARY("TEMPORARY"),
    @Deprecated
    BOUND("BOUND"),
    @Deprecated
    UNBOUND("UNBOUND");

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

    public static boolean isDeleted(String code) {
        return DELETED.code.equals(code);
    }

    public static boolean isActiveLike(String code) {
        return ACTIVE.code.equals(code)
                || TEMPORARY.code.equals(code)
                || BOUND.code.equals(code)
                || UNBOUND.code.equals(code);
    }
}
