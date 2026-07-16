package com.company.officialwebsite.modules.site.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.entity.MediaReferenceEntity;
import com.company.officialwebsite.modules.media.mapper.MediaReferenceMapper;
import com.company.officialwebsite.modules.site.entity.HomeBannerConfigEntity;
import com.company.officialwebsite.modules.site.mapper.HomeBannerConfigMapper;
import com.company.officialwebsite.modules.system.mapper.SysAuditLogMapper;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * HomeBannerControllerTest：验证首页首屏 Banner 后台维护与前台暴露的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class HomeBannerControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MediaReferenceMapper mediaReferenceMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    @Autowired
    private HomeBannerConfigMapper homeBannerConfigMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");

        HomeBannerConfigEntity entity = homeBannerConfigMapper.selectOne(new LambdaQueryWrapper<HomeBannerConfigEntity>()
                .eq(HomeBannerConfigEntity::getConfigKey, "default")
                .eq(HomeBannerConfigEntity::getDeletedMarker, 0L)
                .last("limit 1"));
        Assertions.assertNotNull(entity);

        jdbcTemplate.update(
                "UPDATE cms_home_banner_config SET main_title=?, sub_title=?, background_image_media_id=?, primary_enabled=?, primary_text=?, primary_target_type=?, primary_route_path=?, primary_anchor_code=?, primary_external_url=?, primary_open_in_new_tab=?, secondary_enabled=?, secondary_text=?, secondary_target_type=?, secondary_route_path=?, secondary_anchor_code=?, secondary_external_url=?, secondary_open_in_new_tab=?, version=? WHERE id=?",
                "",
                "",
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                false,
                null,
                null,
                null,
                null,
                null,
                false,
                0,
                entity.getId());
        redisTemplate.delete(portalCacheKeyBuilder.build("home", "banner"));
    }

    @Test
    void adminGetBanner_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/site/home-banner"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void updateBanner_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long imageMediaId = uploadImage(session, "banner.png");

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                imageMediaId,
                                true,
                                "了解解决方案",
                                "INTERNAL_ROUTE",
                                "/solutions",
                                null,
                                null,
                                false,
                                true,
                                "预约交流",
                                "PAGE_ANCHOR",
                                null,
                                "contact-us",
                                null,
                                false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.mainTitle").value(TestConstants.DEFAULT_HOME_BANNER_TITLE))
                .andExpect(jsonPath("$.data.backgroundImageMediaId").value(imageMediaId))
                .andExpect(jsonPath("$.data.primaryButton.enabled").value(true))
                .andExpect(jsonPath("$.data.secondaryButton.anchorCode").value("contact-us"));

        mockMvc.perform(get("/portal/api/site/home-banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.mainTitle").value(TestConstants.DEFAULT_HOME_BANNER_TITLE))
                .andExpect(jsonPath("$.data.primaryButton.routePath").value("/solutions"))
                .andExpect(jsonPath("$.data.secondaryButton.anchorCode").value("contact-us"));

        Assertions.assertEquals(
                1,
                mediaReferenceMapper.selectCount(new LambdaQueryWrapper<MediaReferenceEntity>()
                        .eq(MediaReferenceEntity::getDeletedMarker, 0L)
                        .eq(MediaReferenceEntity::getBizField, "backgroundImage")));
        Assertions.assertEquals(2L, sysAuditLogMapper.selectCount(null));
    }

    @Test
    void updateBanner_shouldReturnLatestBackgroundImageUrl_whenBackgroundImageChanges() throws Exception {
        MockHttpSession session = loginAsAdmin();
        UploadedMedia firstImage = uploadImageWithUrl(session, "banner-old.png");
        UploadedMedia secondImage = uploadImageWithUrl(session, "banner-new.png");

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                firstImage.mediaId(),
                                true,
                                "了解解决方案",
                                "INTERNAL_ROUTE",
                                "/solutions",
                                null,
                                null,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.backgroundImageMediaId").value(firstImage.mediaId()))
                .andExpect(jsonPath("$.data.backgroundImageUrl").value(firstImage.url()));

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                1,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                secondImage.mediaId(),
                                true,
                                "了解解决方案",
                                "INTERNAL_ROUTE",
                                "/solutions",
                                null,
                                null,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.version").value(2))
                .andExpect(jsonPath("$.data.backgroundImageMediaId").value(secondImage.mediaId()))
                .andExpect(jsonPath("$.data.backgroundImageUrl").value(secondImage.url()));
    }

    @Test
    void updateBanner_shouldRejectStaleVersion() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                "第一次保存",
                                "副标题",
                                null,
                                true,
                                "了解更多",
                                "INTERNAL_ROUTE",
                                "/about",
                                null,
                                null,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                "第二次保存",
                                "副标题",
                                null,
                                true,
                                "了解更多",
                                "INTERNAL_ROUTE",
                                "/about",
                                null,
                                null,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void updateBanner_shouldRejectInvalidBackgroundMedia() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                999999L,
                                true,
                                "了解更多",
                                "INTERNAL_ROUTE",
                                "/about",
                                null,
                                null,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_HOME_BANNER_MEDIA_INVALID));
    }

    @Test
    void updateBanner_shouldValidateButtonFieldLengths() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                "T".repeat(121),
                                "S".repeat(501),
                                null,
                                true,
                                "B".repeat(33),
                                "INTERNAL_ROUTE",
                                "/about",
                                null,
                                null,
                                false,
                                true,
                                "C".repeat(33),
                                "PAGE_ANCHOR",
                                null,
                                "a".repeat(65),
                                null,
                                false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID))
                .andExpect(jsonPath("$.data.fieldErrors[*].field").value(hasItems(
                        "mainTitle", "subTitle", "primaryButton.text", "secondaryButton.text", "secondaryButton.anchorCode")));
    }

    @Test
    void updateBanner_shouldRejectInvalidTargetType_whenPrimaryEnabled() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                null,
                                true,
                                "了解更多",
                                "GROUP",
                                null,
                                null,
                                null,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_HOME_BANNER_TARGET_INVALID));
    }

    @Test
    void updateBanner_shouldRejectPrivateExternalUrl() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                null,
                                true,
                                "了解更多",
                                "EXTERNAL_LINK",
                                null,
                                null,
                                "http://127.0.0.1/internal",
                                true,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_HOME_BANNER_TARGET_INVALID));
    }

    @Test
    void updateBanner_shouldRejectIpv6LoopbackExternalUrl() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                null,
                                true,
                                "了解更多",
                                "EXTERNAL_LINK",
                                null,
                                null,
                                "https://[::1]/internal",
                                true,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_HOME_BANNER_TARGET_INVALID));
    }

    @Test
    void updateBanner_shouldClearDisabledButtonPayload() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                null,
                                true,
                                "了解更多",
                                "EXTERNAL_LINK",
                                null,
                                null,
                                "https://example.com",
                                true,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                1,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                null,
                                false,
                                null,
                                null,
                                "/should-clear",
                                null,
                                "https://should-clear.example.com",
                                true,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.primaryButton.enabled").value(false))
                .andExpect(jsonPath("$.data.primaryButton.text").doesNotExist())
                .andExpect(jsonPath("$.data.primaryButton.targetType").doesNotExist())
                .andExpect(jsonPath("$.data.primaryButton.routePath").doesNotExist())
                .andExpect(jsonPath("$.data.primaryButton.externalUrl").doesNotExist());

        mockMvc.perform(get("/portal/api/site/home-banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryButton").doesNotExist());
    }

    @Test
    void portalGetBanner_shouldUseRedisCacheBeforeInvalidation() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                "缓存标题",
                                "副标题",
                                null,
                                true,
                                "了解更多",
                                "INTERNAL_ROUTE",
                                "/about",
                                null,
                                null,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/site/home-banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mainTitle").value("缓存标题"));

        jdbcTemplate.update(
                "UPDATE cms_home_banner_config SET main_title=?, version=? WHERE config_key=? AND deleted_marker=0",
                "数据库新标题",
                1,
                "default");

        mockMvc.perform(get("/portal/api/site/home-banner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.mainTitle").value("缓存标题"));
    }

    @Test
    void portalGetBanner_shouldTranslateDanglingBackgroundMediaToBannerError() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long imageMediaId = uploadImage(session, "banner.png");

        mockMvc.perform(put("/admin/api/site/home-banner")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildBannerJson(
                                0,
                                TestConstants.DEFAULT_HOME_BANNER_TITLE,
                                TestConstants.DEFAULT_HOME_BANNER_SUBTITLE,
                                imageMediaId,
                                true,
                                "了解更多",
                                "INTERNAL_ROUTE",
                                "/about",
                                null,
                                null,
                                false,
                                false,
                                null,
                                null,
                                null,
                                null,
                                null,
                                false)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        jdbcTemplate.update("DELETE FROM media_asset WHERE id=?", imageMediaId);
        redisTemplate.delete(portalCacheKeyBuilder.build("home", "banner"));

        mockMvc.perform(get("/portal/api/site/home-banner"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_HOME_BANNER_MEDIA_INVALID));
    }

    private Long uploadImage(MockHttpSession session, String filename) throws Exception {
        return uploadImageWithUrl(session, filename).mediaId();
    }

    private UploadedMedia uploadImageWithUrl(MockHttpSession session, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/png", TestConstants.PNG_BYTES);
        MvcResult result = mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return new UploadedMedia(
                root.path("data").path("mediaId").asLong(),
                root.path("data").path("url").asText());
    }

    private String buildBannerJson(
            int version,
            String mainTitle,
            String subTitle,
            Long backgroundImageMediaId,
            boolean primaryEnabled,
            String primaryText,
            String primaryTargetType,
            String primaryRoutePath,
            String primaryAnchorCode,
            String primaryExternalUrl,
            boolean primaryOpenInNewTab,
            boolean secondaryEnabled,
            String secondaryText,
            String secondaryTargetType,
            String secondaryRoutePath,
            String secondaryAnchorCode,
            String secondaryExternalUrl,
            boolean secondaryOpenInNewTab) {
        return """
                {
                  "version":%d,
                  "mainTitle":%s,
                  "subTitle":%s,
                  "backgroundImageMediaId":%s,
                  "primaryButton":{
                    "enabled":%s,
                    "text":%s,
                    "targetType":%s,
                    "routePath":%s,
                    "anchorCode":%s,
                    "externalUrl":%s,
                    "openInNewTab":%s
                  },
                  "secondaryButton":{
                    "enabled":%s,
                    "text":%s,
                    "targetType":%s,
                    "routePath":%s,
                    "anchorCode":%s,
                    "externalUrl":%s,
                    "openInNewTab":%s
                  }
                }
                """.formatted(
                version,
                toJsonValue(mainTitle),
                toJsonValue(subTitle),
                backgroundImageMediaId == null ? "null" : backgroundImageMediaId,
                primaryEnabled,
                toJsonValue(primaryText),
                toJsonValue(primaryTargetType),
                toJsonValue(primaryRoutePath),
                toJsonValue(primaryAnchorCode),
                toJsonValue(primaryExternalUrl),
                primaryOpenInNewTab,
                secondaryEnabled,
                toJsonValue(secondaryText),
                toJsonValue(secondaryTargetType),
                toJsonValue(secondaryRoutePath),
                toJsonValue(secondaryAnchorCode),
                toJsonValue(secondaryExternalUrl),
                secondaryOpenInNewTab);
    }

    private String toJsonValue(String value) {
        if (value == null) {
            return "null";
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }

    private record UploadedMedia(Long mediaId, String url) {
    }
}
