package com.company.officialwebsite.modules.lead.service;

/**
 * LeadModuleConstants：统一维护线索模块跨服务共享的固定配置项。
 */
public final class LeadModuleConstants {

    public static final String BIZ_MODULE = "LEAD";
    public static final String TARGET_TYPE = "LEAD";
    public static final String ACTION_CREATE = "CREATE_LEAD";
    public static final String ACTION_VIEW_DETAIL = "VIEW_LEAD_DETAIL";
    public static final String ACTION_UPDATE_STATUS = "UPDATE_LEAD_STATUS";
    public static final String ACTION_EXPORT = "EXPORT_LEAD";

    public static final String RATE_LIMIT_KEY_PREFIX = "official:ratelimit:lead_submit:";
    public static final int RATE_LIMIT_MAX_COUNT = 5;
    public static final long RATE_LIMIT_WINDOW_SECONDS = 3600L;

    public static final int EXPORT_MAX_ROWS = 5000;
    public static final int EXPORT_MAX_SELECTED_IDS = 500;

    public static final int DEMAND_DESCRIPTION_PREVIEW_LENGTH = 100;

    public static final Long ANONYMOUS_USER_ID = 0L;

    private LeadModuleConstants() {}
}
