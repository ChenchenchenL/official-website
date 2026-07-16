package com.company.officialwebsite.modules.site.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.modules.media.entity.MediaReferenceEntity;
import com.company.officialwebsite.modules.media.mapper.MediaReferenceMapper;
import com.company.officialwebsite.modules.site.entity.SiteConfigEntity;
import com.company.officialwebsite.modules.site.mapper.SiteConfigMapper;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
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
 * SiteConfigControllerTest：验证站点配置后台维护与前台暴露的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class SiteConfigControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MediaReferenceMapper mediaReferenceMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    @Autowired
    private SiteConfigMapper siteConfigMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");

        SiteConfigEntity entity = siteConfigMapper.selectOne(new LambdaQueryWrapper<SiteConfigEntity>()
                .eq(SiteConfigEntity::getConfigKey, "default")
                .eq(SiteConfigEntity::getDeletedMarker, 0L)
                .last("limit 1"));
        Assertions.assertNotNull(entity);

        // 测试重置不能依赖乐观锁更新，否则上一个用例已经递增 version 时会导致重置静默失败。
        jdbcTemplate.update(
                "UPDATE cms_site_config SET site_title=?, seo_keywords=?, seo_description=?, brand_slogan=?, brand_tagline=?, logo_light_media_id=?, logo_dark_media_id=?, version=? WHERE id=?",
                "",
                "",
                "",
                "",
                "",
                null,
                null,
                0,
                entity.getId());
        redisTemplate.delete(portalCacheKeyBuilder.build("site-config"));
    }

    @Test
    void adminGetConfig_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/site/config"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void updateConfig_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadLogo(session, "light.png");
        Long darkMediaId = uploadLogo(session, "dark.png");

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(
                                0,
                                TestConstants.DEFAULT_SITE_TITLE,
                                TestConstants.DEFAULT_SEO_KEYWORDS,
                                TestConstants.DEFAULT_SEO_DESCRIPTION,
                                TestConstants.DEFAULT_BRAND_SLOGAN,
                                TestConstants.DEFAULT_BRAND_TAGLINE,
                                mediaId,
                                darkMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.siteTitle").value(TestConstants.DEFAULT_SITE_TITLE))
                .andExpect(jsonPath("$.data.logoLightMediaId").value(mediaId))
                .andExpect(jsonPath("$.data.logoDarkMediaId").value(darkMediaId));

        mockMvc.perform(get("/portal/api/site/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.siteTitle").value(TestConstants.DEFAULT_SITE_TITLE))
                .andExpect(jsonPath("$.data.brandSlogan").value(TestConstants.DEFAULT_BRAND_SLOGAN));

        Assertions.assertEquals(
                2,
                mediaReferenceMapper.selectCount(new LambdaQueryWrapper<MediaReferenceEntity>()
                        .eq(MediaReferenceEntity::getDeletedMarker, 0L)
                        .eq(MediaReferenceEntity::getBizModule, "SITE")
                        .eq(MediaReferenceEntity::getBizObjectId, entityId())
                        .in(MediaReferenceEntity::getBizField, java.util.List.of("logoLight", "logoDark"))));
        Assertions.assertEquals(3L, sysAuditLogMapper.selectCount(null));
    }

    @Test
    void updateConfig_shouldRejectStaleVersion() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadLogo(session, "logo.png");

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(0, "第一次保存", "key", "desc", "主张", "副标语", mediaId, mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(0, "第二次保存", "key", "desc", "主张", "副标语", mediaId, mediaId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void updateConfig_shouldAllowClearingLogos_whenLogoIdsAreNull() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadLogo(session, "clearable.png");

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(
                                0,
                                TestConstants.DEFAULT_SITE_TITLE,
                                TestConstants.DEFAULT_SEO_KEYWORDS,
                                TestConstants.DEFAULT_SEO_DESCRIPTION,
                                TestConstants.DEFAULT_BRAND_SLOGAN,
                                TestConstants.DEFAULT_BRAND_TAGLINE,
                                mediaId,
                                mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.logoLightMediaId").value(mediaId))
                .andExpect(jsonPath("$.data.logoDarkMediaId").value(mediaId));

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(
                                1,
                                TestConstants.DEFAULT_SITE_TITLE,
                                TestConstants.DEFAULT_SEO_KEYWORDS,
                                TestConstants.DEFAULT_SEO_DESCRIPTION,
                                TestConstants.DEFAULT_BRAND_SLOGAN,
                                TestConstants.DEFAULT_BRAND_TAGLINE,
                                null,
                                null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.logoLightMediaId").doesNotExist())
                .andExpect(jsonPath("$.data.logoDarkMediaId").doesNotExist())
                .andExpect(jsonPath("$.data.logoLightUrl").doesNotExist())
                .andExpect(jsonPath("$.data.logoDarkUrl").doesNotExist());

        mockMvc.perform(get("/portal/api/site/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.logoLightUrl").doesNotExist())
                .andExpect(jsonPath("$.data.logoDarkUrl").doesNotExist());

        Assertions.assertEquals(
                0,
                mediaReferenceMapper.selectCount(new LambdaQueryWrapper<MediaReferenceEntity>()
                        .eq(MediaReferenceEntity::getDeletedMarker, 0L)
                        .eq(MediaReferenceEntity::getBizModule, "SITE")
                        .eq(MediaReferenceEntity::getBizObjectId, entityId())
                        .in(MediaReferenceEntity::getBizField, java.util.List.of("logoLight", "logoDark"))));
    }

    @Test
    void updateConfig_shouldReturnForbidden_whenCsrfMissing() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(
                                0,
                                TestConstants.DEFAULT_SITE_TITLE,
                                TestConstants.DEFAULT_SEO_KEYWORDS,
                                TestConstants.DEFAULT_SEO_DESCRIPTION,
                                TestConstants.DEFAULT_BRAND_SLOGAN,
                                TestConstants.DEFAULT_BRAND_TAGLINE,
                                null,
                                null)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_CSRF_INVALID));
    }

    @Test
    void updateConfig_shouldValidateFieldLengths() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(
                                0,
                                "S".repeat(121),
                                "K".repeat(256),
                                "D".repeat(501),
                                "B".repeat(161),
                                "T".repeat(256),
                                null,
                                null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID))
                .andExpect(jsonPath("$.data.fieldErrors[*].field").value(hasItems(
                        "siteTitle", "seoKeywords", "seoDescription", "brandSlogan", "brandTagline")));
    }

    @Test
    void updateConfig_shouldRejectNegativeVersion() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(
                                -1,
                                TestConstants.DEFAULT_SITE_TITLE,
                                TestConstants.DEFAULT_SEO_KEYWORDS,
                                TestConstants.DEFAULT_SEO_DESCRIPTION,
                                TestConstants.DEFAULT_BRAND_SLOGAN,
                                TestConstants.DEFAULT_BRAND_TAGLINE,
                                null,
                                null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID))
                .andExpect(jsonPath("$.data.fieldErrors[*].field").value(hasItems("version")));
    }

    @Test
    void updateConfig_shouldPreserveAttackLikeInputsAsPlainTextData() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadLogo(session, "attack-like.png");

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(
                                0,
                                "  " + TestConstants.SQL_INJECTION_PAYLOAD + "  ",
                                "  " + TestConstants.SQL_INJECTION_PAYLOAD + "  ",
                                "  " + TestConstants.XSS_PAYLOAD + "  ",
                                "  " + TestConstants.XSS_PAYLOAD + "  ",
                                "  " + TestConstants.SQL_INJECTION_PAYLOAD + "  ",
                                mediaId,
                                mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.siteTitle").value(TestConstants.SQL_INJECTION_PAYLOAD))
                .andExpect(jsonPath("$.data.seoDescription").value(TestConstants.XSS_PAYLOAD))
                .andExpect(jsonPath("$.data.brandSlogan").value(TestConstants.XSS_PAYLOAD));

        mockMvc.perform(get("/portal/api/site/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.siteTitle").value(TestConstants.SQL_INJECTION_PAYLOAD))
                .andExpect(jsonPath("$.data.seoDescription").value(TestConstants.XSS_PAYLOAD));
    }

    @Test
    void portalGetConfig_shouldUseRedisCacheBeforeInvalidation() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadLogo(session, "cached.png");

        mockMvc.perform(put("/admin/api/site/config")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildConfigJson(0, "缓存标题", "key", "desc", "主张", "副标语", mediaId, mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/site/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteTitle").value("缓存标题"));

        jdbcTemplate.update(
                "UPDATE cms_site_config SET site_title=?, version=? WHERE config_key=? AND deleted_marker=0",
                "数据库新标题",
                1,
                "default");

        mockMvc.perform(get("/portal/api/site/config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.siteTitle").value("缓存标题"));
    }

    private Long uploadLogo(MockHttpSession session, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/png", TestConstants.PNG_BYTES);
        MvcResult result = mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("mediaId").asLong();
    }

    private Long entityId() {
        SiteConfigEntity entity = siteConfigMapper.selectOne(new LambdaQueryWrapper<SiteConfigEntity>()
                .eq(SiteConfigEntity::getConfigKey, "default")
                .eq(SiteConfigEntity::getDeletedMarker, 0L)
                .last("limit 1"));
        return entity == null ? null : entity.getId();
    }

    private String buildConfigJson(
            int version,
            String siteTitle,
            String seoKeywords,
            String seoDescription,
            String brandSlogan,
            String brandTagline,
            Long logoLightMediaId,
            Long logoDarkMediaId) {
        String lightLogo = logoLightMediaId == null ? "null" : logoLightMediaId.toString();
        String darkLogo = logoDarkMediaId == null ? "null" : logoDarkMediaId.toString();
        return """
                {
                  "version":%d,
                  "siteTitle":%s,
                  "seoKeywords":%s,
                  "seoDescription":%s,
                  "brandSlogan":%s,
                  "brandTagline":%s,
                  "logoLightMediaId":%s,
                  "logoDarkMediaId":%s
                }
                """.formatted(
                version,
                toJsonValue(siteTitle),
                toJsonValue(seoKeywords),
                toJsonValue(seoDescription),
                toJsonValue(brandSlogan),
                toJsonValue(brandTagline),
                lightLogo,
                darkLogo);
    }

    private String toJsonValue(String value) {
        if (value == null) {
            return "null";
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
