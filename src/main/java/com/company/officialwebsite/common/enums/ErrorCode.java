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
    AUTH_LOGIN_FAILED(20003, "AUTH", "用户名或密码错误"),
    AUTH_ACCOUNT_DISABLED(20004, "AUTH", "账号已被禁用"),
    AUTH_CSRF_INVALID(20005, "AUTH", "请求校验失败，请刷新页面后重试"),

    SITE_LOGO_MEDIA_INVALID(30001, "SITE", "站点 Logo 资源不可用"),
    SITE_NAVIGATION_TARGET_INVALID(30002, "SITE", "导航目标配置不合法"),
    SITE_NAVIGATION_LEVEL_INVALID(30003, "SITE", "导航层级不合法"),
    SITE_NAVIGATION_PARENT_INVALID(30004, "SITE", "父菜单不存在或不可挂接"),
    SITE_NAVIGATION_NAME_DUPLICATE(30005, "SITE", "同级菜单名称重复"),
    SITE_HOME_BANNER_MEDIA_INVALID(30006, "SITE", "首页 Banner 背景图资源不可用"),
    SITE_HOME_BANNER_TARGET_INVALID(30007, "SITE", "首页 Banner 按钮跳转配置不合法"),
    SITE_HOME_METRIC_VALUE_INVALID(30008, "SITE", "首页核心指标数值格式不合法"),
    SITE_HONOR_NOT_FOUND(30401, "SITE", "荣誉记录不存在"),
    SITE_HONOR_ICON_INVALID(30402, "SITE", "荣誉图标资源不可用"),
    SITE_HONOR_NAME_DUPLICATE(30403, "SITE", "荣誉名称重复"),

    MEDIA_FILE_INVALID(60001, "MEDIA", "上传文件不符合要求"),

    SYSTEM_ERROR(80000, "SYSTEM", "系统繁忙，请稍后再试");

    private final int code;
    private final String module;
    private final String defaultMessage;

    static {
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
