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
    SITE_CLIENT_LOGO_NOT_FOUND(30501, "SITE", "客户Logo记录不存在"),
    SITE_CLIENT_LOGO_MEDIA_INVALID(30502, "SITE", "客户Logo资源不可用"),
    SITE_CLIENT_LOGO_NAME_DUPLICATE(30503, "SITE", "客户名称重复"),

    SITE_STRENGTH_METRIC_NOT_FOUND(30601, "SITE", "企业实力指标不存在"),
    SITE_STRENGTH_METRIC_ICON_INVALID(30602, "SITE", "指标图标资源不可用"),
    SITE_STRENGTH_METRIC_LABEL_DUPLICATE(30603, "SITE", "业务标签重复"),

    SITE_AI_CARD_NOT_FOUND(30701, "SITE", "AI卡片记录不存在或已被逻辑删除"),
    SITE_AI_CARD_ICON_INVALID(30702, "SITE", "指定的卡片Icon媒体ID不存在或已被逻辑删除"),
    SITE_AI_CARD_NAME_DUPLICATE(30703, "SITE", "AI卡片名称已存在"),

    SITE_UNIVERSITY_NOT_FOUND(31001, "SITE", "合作高校记录不存在或已被逻辑删除"),
    SITE_UNIVERSITY_LOGO_INVALID(31002, "SITE", "合作高校 Logo 媒体 ID 不可用"),
    SITE_UNIVERSITY_NAME_DUPLICATE(31003, "SITE", "合作高校简称或全称在活跃数据中已存在"),

    SITE_RESEARCH_DIRECTION_NOT_FOUND(31101, "SITE", "研发方向记录不存在或已被逻辑删除"),
    SITE_RESEARCH_DIRECTION_ICON_INVALID(31102, "SITE", "研发方向 Icon 媒体 ID 不可用"),
    SITE_RESEARCH_DIRECTION_TITLE_DUPLICATE(31103, "SITE", "研发方向中文标题在活跃数据中已存在"),

    SITE_TIMELINE_NOT_FOUND(31201, "SITE", "时间轴节点不存在或已删除"),
    SITE_TIMELINE_YEAR_INVALID(31202, "SITE", "年份格式不合法"),
    SITE_TIMELINE_TITLE_DUPLICATE(31203, "SITE", "同一年份标题重复"),

    SITE_VALUE_CARD_NOT_FOUND(31301, "SITE", "核心价值观卡片不存在或已被逻辑删除"),
    SITE_VALUE_CARD_ICON_INVALID(31302, "SITE", "核心价值观图标资源不可用"),
    SITE_VALUE_CARD_TITLE_DUPLICATE(31303, "SITE", "核心价值观标题已存在"),

    SITE_PROMISE_CONTENT_NOT_FOUND(31401, "SITE", "我们的承诺主体文案配置不存在"),
    SITE_PROMISE_TAG_NOT_FOUND(31402, "SITE", "我们的承诺标签不存在或已被逻辑删除"),
    SITE_PROMISE_TAG_TEXT_DUPLICATE(31403, "SITE", "我们的承诺标签文本重复"),

    SITE_CAPABILITY_CATEGORY_NOT_FOUND(30801, "SITE", "核心能力底座分类记录不存在或已被逻辑删除"),
    SITE_CAPABILITY_CATEGORY_NAME_DUPLICATE(30802, "SITE", "已存在重名底座分类名称"),
    SITE_CAPABILITY_ITEM_NOT_FOUND(30901, "SITE", "底座子项记录不存在或已被逻辑删除"),
    SITE_CAPABILITY_ITEM_NAME_DUPLICATE(30902, "SITE", "同一底座分类下已存在同名子项名称"),

    PRODUCT_NOT_FOUND(40001, "PRODUCT", "产品记录不存在或已被逻辑删除"),
    PRODUCT_LOGO_INVALID(40002, "PRODUCT", "指定的产品 Logo 媒体 ID 在 media_file 库中不存在或已被逻辑删除"),
    PRODUCT_NAME_DUPLICATE(40003, "PRODUCT", "产品名称与库中已存在的活跃产品重名"),
    PRODUCT_SOLUTION_NOT_FOUND(40101, "PRODUCT", "行业解决方案记录不存在或已被逻辑删除"),
    PRODUCT_SOLUTION_ICON_INVALID(40102, "PRODUCT", "行业解决方案的 Icon 媒体 ID 不可用"),
    PRODUCT_SOLUTION_NAME_DUPLICATE(40103, "PRODUCT", "行业解决方案名称已存在"),

    CASE_NOT_FOUND(40201, "CASE", "标杆案例不存在或已被逻辑删除"),
    CASE_LOGO_INVALID(40202, "CASE", "标杆案例的封面/Logo 媒体 ID 不可用"),
    CASE_TITLE_DUPLICATE(40203, "CASE", "标杆案例标题已存在"),

    MEDIA_FILE_INVALID(60001, "MEDIA", "上传文件不符合要求"),
    MEDIA_FILE_SIZE_EXCEEDED(60002, "MEDIA", "文件大小超出限制"),
    MEDIA_FILE_TYPE_UNSUPPORTED(60003, "MEDIA", "文件类型不支持"),
    MEDIA_FILE_SIGNATURE_INVALID(60004, "MEDIA", "文件内容签名不合法"),
    MEDIA_UPLOAD_FAILED(60005, "MEDIA", "文件上传失败"),
    MEDIA_STORAGE_WRITE_FAILED(60006, "MEDIA", "文件存储失败"),

    LEAD_CONTACT_INFO_NOT_FOUND(70001, "LEAD", "基础联系方式配置不存在"),
    LEAD_COOPERATION_DIRECTION_TAG_NOT_FOUND(70002, "LEAD", "合作方向标签不存在或已被逻辑删除"),
    LEAD_COOPERATION_DIRECTION_TAG_TEXT_DUPLICATE(70003, "LEAD", "合作方向标签文本重复"),
    LEAD_RECORD_NOT_FOUND(70004, "LEAD", "线索记录不存在或已删除"),
    LEAD_STATUS_INVALID(70005, "LEAD", "线索状态不合法"),
    LEAD_EXPORT_TOO_LARGE(70006, "LEAD", "导出数据量超过上限"),
    LEAD_SUBMIT_RATE_LIMITED(70007, "LEAD", "同一 IP 提交过于频繁"),

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
