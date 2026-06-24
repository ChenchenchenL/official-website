package com.company.officialwebsite.common.constants;

/**
 * SecurityConstants：统一维护鉴权相关的接口路径、角色前缀和固定安全常量。
 */
public final class SecurityConstants {

    public static final String ADMIN_API_PATTERN = "/admin/api/**";
    public static final String PORTAL_API_PATTERN = "/portal/api/**";
    public static final String AUTH_BASE_PATH = "/admin/api/auth";
    public static final String CSRF_ENDPOINT = "/admin/api/auth/csrf";
    public static final String LOGIN_ENDPOINT = "/admin/api/auth/login";
    public static final String ROLE_PREFIX = "ROLE_";

    private SecurityConstants() {
    }
}
