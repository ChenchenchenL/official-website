package com.company.officialwebsite.modules.site.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.site.mapper.PartnerUniversityMapper;
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
 * PartnerUniversityControllerTest：验证合作高校管理与展示接口的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PartnerUniversityControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PartnerUniversityMapper partnerUniversityMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE biz_module = 'SITE' AND biz_field = 'logo'");
        jdbcTemplate.update("DELETE FROM cms_partner_university");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build("partner_universities"));
    }

    @Test
    void adminGetUniversities_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/partner-universities"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void portalGetUniversities_shouldReturnVisibleData_inSortOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long media1 = uploadImage(session, "u1.png");
        Long media2 = uploadImage(session, "u2.png");
        createUniversity(session, media1, "华科", "华中科技大学", true);
        createUniversity(session, media2, "武大", "武汉大学", true);
        redisTemplate.delete(portalCacheKeyBuilder.build("partner_universities"));

        mockMvc.perform(get("/portal/api/partner-universities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].name").value("华科"));
    }

    @Test
    void createUniversity_shouldPersistBindMediaAndInvalidateCache() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "hust.png");

        MvcResult result = mockMvc.perform(post("/admin/api/partner-universities")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"华科","fullName":"华中科技大学","logoMediaId":%d,"visible":true}
                                """.formatted(mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertTrue(node.path("data").isArray());
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(), mediaAssetMapper.selectById(mediaId).getStatus());
        Assertions.assertEquals(2L, sysAuditLogMapper.selectCount(null));
        Assertions.assertNull(redisTemplate.opsForValue().get(portalCacheKeyBuilder.build("partner_universities")));
    }

    @Test
    void createUniversity_shouldRejectDuplicateName() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long media1 = uploadImage(session, "dup1.png");
        Long media2 = uploadImage(session, "dup2.png");
        createUniversity(session, media1, "华科", "华中科技大学", true);

        mockMvc.perform(post("/admin/api/partner-universities")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"华科","fullName":"新的全称","logoMediaId":%d,"visible":true}
                                """.formatted(media2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_UNIVERSITY_NAME_DUPLICATE));
    }

    @Test
    void createUniversity_shouldRejectInvalidMedia() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(post("/admin/api/partner-universities")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"华科","fullName":"华中科技大学","logoMediaId":999999,"visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_UNIVERSITY_LOGO_INVALID));
    }

    @Test
    void updateUniversity_shouldMoveMediaBindingAndHidePortalItem() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long oldMedia = uploadImage(session, "old.png");
        Long newMedia = uploadImage(session, "new.png");
        Long universityId = createUniversity(session, oldMedia, "华科", "华中科技大学", true);
        int version = currentVersion(universityId);

        mockMvc.perform(put("/admin/api/partner-universities/{id}", universityId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":%d,"name":"华科-更新","fullName":"华中科技大学-更新","logoMediaId":%d,"visible":false}
                                """.formatted(version, newMedia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/partner-universities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(), mediaAssetMapper.selectById(oldMedia).getStatus());
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(), mediaAssetMapper.selectById(newMedia).getStatus());
    }

    @Test
    void deleteUniversity_shouldUnbindMedia() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "delete.png");
        Long universityId = createUniversity(session, mediaId, "华科", "华中科技大学", true);
        int version = currentVersion(universityId);

        mockMvc.perform(delete("/admin/api/partner-universities/{id}", universityId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(), mediaAssetMapper.selectById(mediaId).getStatus());
        Assertions.assertEquals(0L, partnerUniversityMapper.selectCount(null));
    }

    @Test
    void reorderUniversity_shouldPersistRequestedOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long first = createUniversity(session, uploadImage(session, "a.png"), "华科", "华中科技大学", true);
        Long second = createUniversity(session, uploadImage(session, "b.png"), "武大", "武汉大学", true);

        mockMvc.perform(post("/admin/api/partner-universities/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedIds":[%d,%d]}
                                """.formatted(second, first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/partner-universities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("武大"));
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

    private Long createUniversity(MockHttpSession session, Long mediaId, String name, String fullName, boolean visible) throws Exception {
        mockMvc.perform(post("/admin/api/partner-universities")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","fullName":"%s","logoMediaId":%d,"visible":%s}
                                """.formatted(name, fullName, mediaId, visible)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cms_partner_university WHERE name = ? AND deleted_marker = 0",
                Long.class,
                name);
    }

    private int currentVersion(Long universityId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_partner_university WHERE id = ?",
                Integer.class,
                universityId);
        return version == null ? 0 : version;
    }
}
