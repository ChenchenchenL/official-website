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
import com.company.officialwebsite.modules.site.entity.HonorEntity;
import com.company.officialwebsite.modules.site.mapper.HonorMapper;
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
 * HonorControllerTest：验证企业荣誉标签后台维护与前台暴露的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class HonorControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private HonorMapper honorMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE id > 0");
        jdbcTemplate.update("DELETE FROM cms_honor WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        jdbcTemplate.update("UPDATE cms_honor SET visible = 1, sort_order = CASE id WHEN -9201 THEN 10 WHEN -9202 THEN 20 WHEN -9203 THEN 30 WHEN -9204 THEN 40 END WHERE id IN (-9201, -9202, -9203, -9204)");
        jdbcTemplate.update("UPDATE media_asset SET status = 'BOUND' WHERE id IN (-9101, -9102, -9103, -9104)");
        redisTemplate.delete(portalCacheKeyBuilder.build("honors"));
    }

    @Test
    void adminGetHonors_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/site/honors"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void portalGetHonors_shouldReturnDefaultSeedHonors() throws Exception {
        mockMvc.perform(get("/portal/api/site/honors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].name").value("国家高新技术企业"))
                .andExpect(jsonPath("$.data[3].name").value("中国光谷3551企业"));
    }

    @Test
    void createHonor_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "honor.png");

        mockMvc.perform(post("/admin/api/site/honors")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"国家级信创生态单位","iconId":%d,"visible":true}
                                """.formatted(mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[*].name").value(hasItems("国家级信创生态单位")));

        mockMvc.perform(get("/portal/api/site/honors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name").value(hasItems("国家级信创生态单位")));

        MediaAssetEntity asset = mediaAssetMapper.selectById(mediaId);
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(), asset.getStatus());
        Assertions.assertEquals(1L, sysAuditLogMapper.selectCount(new LambdaQueryWrapper<>()));
    }

    @Test
    void createHonor_shouldRejectDuplicateName() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "dup.png");

        mockMvc.perform(post("/admin/api/site/honors")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"国家高新技术企业","iconId":%d,"visible":true}
                                """.formatted(mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_HONOR_NAME_DUPLICATE));
    }

    @Test
    void createHonor_shouldRejectInvalidIcon() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/honors")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"测试荣誉","iconId":999999,"visible":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_HONOR_ICON_INVALID));
    }

    @Test
    void updateHonor_shouldMoveMediaBindingAndAllowHide() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long oldMediaId = uploadImage(session, "old.png");
        Long newMediaId = uploadImage(session, "new.png");
        Long honorId = createHonor(session, oldMediaId, "待更新荣誉", true);
        int version = currentVersion(honorId);

        mockMvc.perform(put("/admin/api/site/honors/{honorId}", honorId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":%d,"name":"待更新荣誉-新版","iconId":%d,"visible":false}
                                """.formatted(version, newMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[*].name").value(hasItems("待更新荣誉-新版")));

        mockMvc.perform(get("/portal/api/site/honors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].name").value(org.hamcrest.Matchers.not(hasItems("待更新荣誉-新版"))));

        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(), mediaAssetMapper.selectById(oldMediaId).getStatus());
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(), mediaAssetMapper.selectById(newMediaId).getStatus());
    }

    @Test
    void deleteHonor_shouldUnbindMedia() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "delete.png");
        Long honorId = createHonor(session, mediaId, "待删除荣誉", true);
        int version = currentVersion(honorId);

        mockMvc.perform(delete("/admin/api/site/honors/{honorId}", honorId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_honor WHERE id = ? AND deleted_marker = 0",
                Integer.class,
                honorId);
        Assertions.assertEquals(0, activeCount == null ? 0 : activeCount);
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(), mediaAssetMapper.selectById(mediaId).getStatus());
    }

    @Test
    void reorderHonors_shouldRejectIncompleteSet() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/honors/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedHonorIds":[-9201,-9202]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void reorderHonors_shouldPersistRequestedOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/honors/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedHonorIds":[-9204,-9203,-9202,-9201]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[0].name").value("中国光谷3551企业"));

        mockMvc.perform(get("/portal/api/site/honors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("中国光谷3551企业"));
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

    private Long createHonor(MockHttpSession session, Long iconId, String name, boolean visible) throws Exception {
        mockMvc.perform(post("/admin/api/site/honors")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","iconId":%d,"visible":%s}
                                """.formatted(name, iconId, visible)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        return jdbcTemplate.queryForObject("SELECT id FROM cms_honor WHERE name = ? AND deleted_marker = 0", Long.class, name);
    }

    private int currentVersion(Long honorId) {
        Integer version = jdbcTemplate.queryForObject("SELECT version FROM cms_honor WHERE id = ?", Integer.class, honorId);
        return version == null ? 0 : version;
    }
}
