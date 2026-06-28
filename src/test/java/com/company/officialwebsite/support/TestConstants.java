package com.company.officialwebsite.support;

import com.company.officialwebsite.common.enums.ErrorCode;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * TestConstants：集中维护控制器集成测试共用的凭证、错误码和样例媒体字节。
 */
public final class TestConstants {

    public static final String ADMIN_USERNAME = "admin";
    public static final String ADMIN_PASSWORD = "Admin@123456";

    public static final int SUCCESS = ErrorCode.SUCCESS.getCode();
    public static final int PARAM_INVALID = ErrorCode.COMMON_PARAM_INVALID.getCode();
    public static final int STATE_CONFLICT = ErrorCode.COMMON_STATE_CONFLICT.getCode();
    public static final int AUTH_UNAUTHORIZED = ErrorCode.AUTH_UNAUTHORIZED.getCode();
    public static final int AUTH_CSRF_INVALID = ErrorCode.AUTH_CSRF_INVALID.getCode();
    public static final int MEDIA_FILE_INVALID = ErrorCode.MEDIA_FILE_INVALID.getCode();
    public static final int MEDIA_FILE_SIZE_EXCEEDED = ErrorCode.MEDIA_FILE_SIZE_EXCEEDED.getCode();
    public static final int MEDIA_FILE_TYPE_UNSUPPORTED = ErrorCode.MEDIA_FILE_TYPE_UNSUPPORTED.getCode();
    public static final int MEDIA_FILE_SIGNATURE_INVALID = ErrorCode.MEDIA_FILE_SIGNATURE_INVALID.getCode();
    public static final int MEDIA_UPLOAD_FAILED = ErrorCode.MEDIA_UPLOAD_FAILED.getCode();
    public static final int MEDIA_STORAGE_WRITE_FAILED = ErrorCode.MEDIA_STORAGE_WRITE_FAILED.getCode();
    public static final int SITE_NAVIGATION_TARGET_INVALID = ErrorCode.SITE_NAVIGATION_TARGET_INVALID.getCode();
    public static final int SITE_NAVIGATION_NAME_DUPLICATE = ErrorCode.SITE_NAVIGATION_NAME_DUPLICATE.getCode();
    public static final int SITE_HOME_BANNER_MEDIA_INVALID = ErrorCode.SITE_HOME_BANNER_MEDIA_INVALID.getCode();
    public static final int SITE_HOME_BANNER_TARGET_INVALID = ErrorCode.SITE_HOME_BANNER_TARGET_INVALID.getCode();
    public static final int SITE_HOME_METRIC_VALUE_INVALID = ErrorCode.SITE_HOME_METRIC_VALUE_INVALID.getCode();
    public static final int SITE_HONOR_NOT_FOUND = ErrorCode.SITE_HONOR_NOT_FOUND.getCode();
    public static final int SITE_HONOR_ICON_INVALID = ErrorCode.SITE_HONOR_ICON_INVALID.getCode();
    public static final int SITE_HONOR_NAME_DUPLICATE = ErrorCode.SITE_HONOR_NAME_DUPLICATE.getCode();
    public static final int SITE_CLIENT_LOGO_NOT_FOUND = ErrorCode.SITE_CLIENT_LOGO_NOT_FOUND.getCode();
    public static final int SITE_CLIENT_LOGO_MEDIA_INVALID = ErrorCode.SITE_CLIENT_LOGO_MEDIA_INVALID.getCode();
    public static final int SITE_CLIENT_LOGO_NAME_DUPLICATE = ErrorCode.SITE_CLIENT_LOGO_NAME_DUPLICATE.getCode();
    public static final int SITE_STRENGTH_METRIC_NOT_FOUND = ErrorCode.SITE_STRENGTH_METRIC_NOT_FOUND.getCode();
    public static final int SITE_STRENGTH_METRIC_ICON_INVALID = ErrorCode.SITE_STRENGTH_METRIC_ICON_INVALID.getCode();
    public static final int SITE_STRENGTH_METRIC_LABEL_DUPLICATE = ErrorCode.SITE_STRENGTH_METRIC_LABEL_DUPLICATE.getCode();
    public static final int SITE_AI_CARD_NOT_FOUND = ErrorCode.SITE_AI_CARD_NOT_FOUND.getCode();
    public static final int SITE_AI_CARD_ICON_INVALID = ErrorCode.SITE_AI_CARD_ICON_INVALID.getCode();
    public static final int SITE_AI_CARD_NAME_DUPLICATE = ErrorCode.SITE_AI_CARD_NAME_DUPLICATE.getCode();
    public static final int SITE_UNIVERSITY_NOT_FOUND = ErrorCode.SITE_UNIVERSITY_NOT_FOUND.getCode();
    public static final int SITE_UNIVERSITY_LOGO_INVALID = ErrorCode.SITE_UNIVERSITY_LOGO_INVALID.getCode();
    public static final int SITE_UNIVERSITY_NAME_DUPLICATE = ErrorCode.SITE_UNIVERSITY_NAME_DUPLICATE.getCode();
    public static final int SITE_RESEARCH_DIRECTION_NOT_FOUND = ErrorCode.SITE_RESEARCH_DIRECTION_NOT_FOUND.getCode();
    public static final int SITE_RESEARCH_DIRECTION_ICON_INVALID = ErrorCode.SITE_RESEARCH_DIRECTION_ICON_INVALID.getCode();
    public static final int SITE_RESEARCH_DIRECTION_TITLE_DUPLICATE = ErrorCode.SITE_RESEARCH_DIRECTION_TITLE_DUPLICATE.getCode();
    public static final int SITE_TIMELINE_NOT_FOUND = ErrorCode.SITE_TIMELINE_NOT_FOUND.getCode();
    public static final int SITE_TIMELINE_YEAR_INVALID = ErrorCode.SITE_TIMELINE_YEAR_INVALID.getCode();
    public static final int SITE_TIMELINE_TITLE_DUPLICATE = ErrorCode.SITE_TIMELINE_TITLE_DUPLICATE.getCode();
    public static final int SITE_VALUE_CARD_NOT_FOUND = ErrorCode.SITE_VALUE_CARD_NOT_FOUND.getCode();
    public static final int SITE_VALUE_CARD_ICON_INVALID = ErrorCode.SITE_VALUE_CARD_ICON_INVALID.getCode();
    public static final int SITE_VALUE_CARD_TITLE_DUPLICATE = ErrorCode.SITE_VALUE_CARD_TITLE_DUPLICATE.getCode();
    public static final int SITE_PROMISE_CONTENT_NOT_FOUND = ErrorCode.SITE_PROMISE_CONTENT_NOT_FOUND.getCode();
    public static final int SITE_PROMISE_TAG_NOT_FOUND = ErrorCode.SITE_PROMISE_TAG_NOT_FOUND.getCode();
    public static final int SITE_PROMISE_TAG_TEXT_DUPLICATE = ErrorCode.SITE_PROMISE_TAG_TEXT_DUPLICATE.getCode();
    public static final int PRODUCT_SOLUTION_NOT_FOUND = ErrorCode.PRODUCT_SOLUTION_NOT_FOUND.getCode();
    public static final int PRODUCT_SOLUTION_ICON_INVALID = ErrorCode.PRODUCT_SOLUTION_ICON_INVALID.getCode();
    public static final int PRODUCT_SOLUTION_NAME_DUPLICATE = ErrorCode.PRODUCT_SOLUTION_NAME_DUPLICATE.getCode();
    public static final int CASE_NOT_FOUND = ErrorCode.CASE_NOT_FOUND.getCode();
    public static final int CASE_LOGO_INVALID = ErrorCode.CASE_LOGO_INVALID.getCode();
    public static final int CASE_TITLE_DUPLICATE = ErrorCode.CASE_TITLE_DUPLICATE.getCode();
    public static final int LEAD_CONTACT_INFO_NOT_FOUND = ErrorCode.LEAD_CONTACT_INFO_NOT_FOUND.getCode();
    public static final int LEAD_COOPERATION_DIRECTION_TAG_NOT_FOUND =
            ErrorCode.LEAD_COOPERATION_DIRECTION_TAG_NOT_FOUND.getCode();
    public static final int LEAD_COOPERATION_DIRECTION_TAG_TEXT_DUPLICATE =
            ErrorCode.LEAD_COOPERATION_DIRECTION_TAG_TEXT_DUPLICATE.getCode();
    public static final int LEAD_RECORD_NOT_FOUND = ErrorCode.LEAD_RECORD_NOT_FOUND.getCode();
    public static final int LEAD_STATUS_INVALID = ErrorCode.LEAD_STATUS_INVALID.getCode();
    public static final int LEAD_EXPORT_TOO_LARGE = ErrorCode.LEAD_EXPORT_TOO_LARGE.getCode();
    public static final int LEAD_SUBMIT_RATE_LIMITED = ErrorCode.LEAD_SUBMIT_RATE_LIMITED.getCode();
    public static final int COMMON_REQUEST_TOO_FREQUENT = ErrorCode.COMMON_REQUEST_TOO_FREQUENT.getCode();

    public static final String DEFAULT_SITE_TITLE = "测试官网标题";
    public static final String DEFAULT_SEO_KEYWORDS = "AI,Data,Testing";
    public static final String DEFAULT_SEO_DESCRIPTION = "用于控制器集成测试的站点配置描述";
    public static final String DEFAULT_BRAND_SLOGAN = "让组织拥有持续进化的数字智能能力";
    public static final String DEFAULT_BRAND_TAGLINE = "数据驱动增长";
    public static final String DEFAULT_HOME_BANNER_TITLE = "让组织拥有持续进化的数字智能能力";
    public static final String DEFAULT_HOME_BANNER_SUBTITLE = "聚焦企业数字化、数据智能与 AI 应用落地";
    public static final String XSS_PAYLOAD = "<script>alert('xss')</script>";
    public static final String SQL_INJECTION_PAYLOAD = "' OR '1'='1";

    public static final byte[] PNG_BYTES = createMinimalPngBytes();
    public static final byte[] SVG_BYTES = """
            <svg xmlns="http://www.w3.org/2000/svg" width="1" height="1">
              <script>alert('xss')</script>
            </svg>
            """.getBytes(StandardCharsets.UTF_8);

    private TestConstants() {
    }

    public static byte[] oversizedPngBytes(long maxAllowedBytes) {
        int targetSize = Math.toIntExact(maxAllowedBytes + 1);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(targetSize);
        outputStream.writeBytes(PNG_BYTES);
        outputStream.writeBytes(new byte[targetSize - PNG_BYTES.length]);
        return outputStream.toByteArray();
    }

    private static byte[] createMinimalPngBytes() {
        return new byte[] {
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
                0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
                0x08, 0x02, 0x00, 0x00, 0x00, (byte) 0x90, 0x77, 0x53, (byte) 0xDE,
                0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54,
                0x08, (byte) 0xD7, 0x63, (byte) 0xF8, (byte) 0xCF, (byte) 0xC0, 0x00, 0x00,
                0x03, 0x01, 0x01, 0x00, 0x18, (byte) 0xDD, (byte) 0x8D, (byte) 0xB0,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44,
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82
        };
    }
}
