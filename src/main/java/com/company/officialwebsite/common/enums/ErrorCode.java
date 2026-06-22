package com.company.officialwebsite.common.enums;

import java.util.HashSet;
import java.util.Set;

/**
 * ErrorCode：统一维护接口返回的数字业务状态码和默认提示。
 */
public enum ErrorCode {

    SUCCESS(0, "COMMON", "操作成功"),

    COMMON_PARAM_INVALID(10001, "COMMON", "请求参数不正确"),
    COMMON_RESOURCE_NOT_FOUND(10002, "COMMON", "资源不存在"),
    COMMON_STATE_CONFLICT(10003, "COMMON", "当前状态不允许执行该操作"),
    COMMON_DUPLICATE_DATA(10004, "COMMON", "数据已存在"),
    COMMON_REQUEST_TOO_FREQUENT(10005, "COMMON", "请求过于频繁"),

    AUTH_UNAUTHORIZED(20001, "AUTH", "请先登录"),
    AUTH_FORBIDDEN(20002, "AUTH", "无权执行该操作"),

    SYSTEM_ERROR(80000, "SYSTEM", "系统繁忙，请稍后再试");

    /*
     * 预留编码段：
     * 30000-39999 站点、导航、首页与 AI 能力
     * 40000-49999 产品
     * 50000-59999 行业、案例与新闻
     * 60000-69999 媒体资源
     * 70000-79999 客户线索
     * 80000-89999 审计、缓存与系统支撑
     * 90000-99999 第三方集成或扩展预留
     */

    private final int code;
    private final String module;
    private final String defaultMessage;

    static {
        // 启动时阻断重复数字码，避免多人协作时无意复用已对外承诺的错误码。
        Set<Integer> usedCodes = new HashSet<>();
        for (ErrorCode errorCode : values()) {
            if (!usedCodes.add(errorCode.code)) {
                throw new IllegalStateException("Duplicate error code: " + errorCode.code);
            }
        }
    }

    ErrorCode(int code, String module, String defaultMessage) {
        this.code = code;
        this.module = module;
        this.defaultMessage = defaultMessage;
    }

    public int getCode() {
        return code;
    }

    public String getModule() {
        return module;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public boolean isSuccess() {
        return this == SUCCESS;
    }
}
