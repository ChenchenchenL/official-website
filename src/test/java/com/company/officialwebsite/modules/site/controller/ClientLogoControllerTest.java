package com.company.officialwebsite.modules.site.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.site.mapper.ClientLogoMapper;
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
 * ClientLogoControllerTest：验证服务客户 Logo 墙后台维护与前台暴露的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ClientLogoControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ClientLogoMapper clientLogoMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE biz_field = 'logo' AND biz_module = 'SITE'");
        jdbcTemplate.update("DELETE FROM cms_client_logo");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build("client_logos"));
    }

    @Test
    void adminGetClientLogos_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/site/client-logos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void createClientLogo_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "client.png");

        mockMvc.perform(post("/admin/api/site/client-logos")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"中国核电","industry":"新能源与电力","logoId":%d,"visible":true,"sortOrder":2}
                                """.formatted(mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data").isNumber());

        mockMvc.perform(get("/admin/api/site/client-logos").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("中国核电"));

        mockMvc.perform(get("/portal/api/site/client-logos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name").value(hasItems("中国核电")));

        MediaAssetEntity asset = mediaAssetMapper.selectById(mediaId);
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(), asset.getStatus());
        Assertions.assertEquals(2L, sysAuditLogMapper.selectCount(new LambdaQueryWrapper<>()));
    }

    @Test
    void createClientLogo_shouldRejectDuplicateName() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long firstMediaId = uploadImage(session, "dup-1.png");
        Long secondMediaId = uploadImage(session, "dup-2.png");
        createClientLogo(session, firstMediaId, "天山材料", true, 10);

        mockMvc.perform(post("/admin/api/site/client-logos")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"天山材料","industry":"建材","logoId":%d,"visible":true,"sortOrder":20}
                                """.formatted(secondMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_CLIENT_LOGO_NAME_DUPLICATE));
    }

    @Test
    void createClientLogo_shouldRejectInvalidLogo() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/client-logos")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"测试客户","industry":"能源","logoId":999999,"visible":true,"sortOrder":20}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_CLIENT_LOGO_MEDIA_INVALID));
    }

    @Test
    void updateClientLogo_shouldMoveMediaBindingAndAllowHide() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long oldMediaId = uploadImage(session, "old-logo.png");
        Long newMediaId = uploadImage(session, "new-logo.png");
        Long clientLogoId = createClientLogo(session, oldMediaId, "待更新客户", true, 10);
        int version = currentVersion(clientLogoId);

        mockMvc.perform(put("/admin/api/site/client-logos/{clientLogoId}", clientLogoId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":%d,"name":"待更新客户-新版","industry":"清洁能源","logoId":%d,"visible":false,"sortOrder":5}
                                """.formatted(version, newMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/site/client-logos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name").value(org.hamcrest.Matchers.not(hasItems("待更新客户-新版"))));

        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(), mediaAssetMapper.selectById(oldMediaId).getStatus());
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(), mediaAssetMapper.selectById(newMediaId).getStatus());
    }

    @Test
    void deleteClientLogo_shouldUnbindMedia() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "delete-logo.png");
        Long clientLogoId = createClientLogo(session, mediaId, "待删除客户", true, 10);
        int version = currentVersion(clientLogoId);

        mockMvc.perform(delete("/admin/api/site/client-logos/{clientLogoId}", clientLogoId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_client_logo WHERE id = ? AND deleted_marker = 0",
                Integer.class,
                clientLogoId);
        Assertions.assertEquals(0, activeCount == null ? 0 : activeCount);
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(), mediaAssetMapper.selectById(mediaId).getStatus());
    }

    @Test
    void batchSortClientLogos_shouldRejectIncompleteSet() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long first = createClientLogo(session, uploadImage(session, "sort-1.png"), "客户A", true, 10);
        createClientLogo(session, uploadImage(session, "sort-2.png"), "客户B", true, 20);

        mockMvc.perform(put("/admin/api/site/client-logos/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"id":%d,"sortOrder":1}]
                                """.formatted(first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void batchSortClientLogos_shouldPersistRequestedOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long first = createClientLogo(session, uploadImage(session, "order-1.png"), "客户A", true, 10);
        Long second = createClientLogo(session, uploadImage(session, "order-2.png"), "客户B", true, 20);
        Long third = createClientLogo(session, uploadImage(session, "order-3.png"), "客户C", true, 30);

        mockMvc.perform(put("/admin/api/site/client-logos/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [{"id":%d,"sortOrder":3},{"id":%d,"sortOrder":2},{"id":%d,"sortOrder":1}]
                                """.formatted(first, second, third)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/site/client-logos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("客户C"));
    }

    private Long uploadImage(MockHttpSession session, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/png", TestConstants.PNG_BYTES);
        MvcResult result = mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("mediaId").asLong();
    }

    private Long createClientLogo(MockHttpSession session, Long logoId, String name, boolean visible, int sortOrder) throws Exception {
        mockMvc.perform(post("/admin/api/site/client-logos")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","industry":"测试行业","logoId":%d,"visible":%s,"sortOrder":%d}
                                """.formatted(name, logoId, visible, sortOrder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cms_client_logo WHERE name = ? AND deleted_marker = 0",
                Long.class,
                name);
    }

    private int currentVersion(Long clientLogoId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_client_logo WHERE id = ?",
                Integer.class,
                clientLogoId);
        return version == null ? 0 : version;
    }
}
