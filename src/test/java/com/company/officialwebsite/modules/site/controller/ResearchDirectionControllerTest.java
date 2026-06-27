package com.company.officialwebsite.modules.site.controller;

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
import com.company.officialwebsite.modules.site.mapper.ResearchDirectionMapper;
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
 * ResearchDirectionControllerTest：验证重点研发方向管理与展示接口的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ResearchDirectionControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResearchDirectionMapper researchDirectionMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE biz_module = 'SITE' AND biz_field = 'icon'");
        jdbcTemplate.update("DELETE FROM cms_research_direction");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build("research_directions"));
    }

    @Test
    void adminGetDirections_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/research-directions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void portalGetDirections_shouldReturnVisibleData_inSortOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long media1 = uploadImage(session, "d1.png");
        Long media2 = uploadImage(session, "d2.png");
        createDirection(session, media1, "数据智能", "Data Intelligence", "A", true);
        createDirection(session, media2, "知识工程", "Knowledge Engineering", "B", true);
        redisTemplate.delete(portalCacheKeyBuilder.build("research_directions"));

        mockMvc.perform(get("/portal/api/research-directions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].titleCn").value("数据智能"));
    }

    @Test
    void createDirection_shouldPersistBindMediaAndInvalidateCache() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "ai.png");

        MvcResult result = mockMvc.perform(post("/admin/api/research-directions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleCn":"数据智能","titleEn":"Data Intelligence","summary":"测试摘要","iconMediaId":%d,"visible":true}
                                """.formatted(mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();

        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertTrue(node.path("data").isArray());
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(), mediaAssetMapper.selectById(mediaId).getStatus());
        Assertions.assertEquals(1L, sysAuditLogMapper.selectCount(null));
        Assertions.assertNull(redisTemplate.opsForValue().get(portalCacheKeyBuilder.build("research_directions")));
    }

    @Test
    void createDirection_shouldRejectDuplicateTitle() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long media1 = uploadImage(session, "dup1.png");
        Long media2 = uploadImage(session, "dup2.png");
        createDirection(session, media1, "数据智能", "Data Intelligence", "A", true);

        mockMvc.perform(post("/admin/api/research-directions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleCn":"数据智能","titleEn":"New English","summary":"测试摘要","iconMediaId":%d,"visible":true}
                                """.formatted(media2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_RESEARCH_DIRECTION_TITLE_DUPLICATE));
    }

    @Test
    void createDirection_shouldRejectInvalidMedia() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(post("/admin/api/research-directions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleCn":"数据智能","titleEn":"Data Intelligence","summary":"测试摘要","iconMediaId":999999,"visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_RESEARCH_DIRECTION_ICON_INVALID));
    }

    @Test
    void updateDirection_shouldMoveMediaBindingAndHidePortalItem() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long oldMedia = uploadImage(session, "old.png");
        Long newMedia = uploadImage(session, "new.png");
        Long directionId = createDirection(session, oldMedia, "数据智能", "Data Intelligence", "A", true);
        int version = currentVersion(directionId);

        mockMvc.perform(put("/admin/api/research-directions/{id}", directionId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":%d,"titleCn":"数据智能-更新","titleEn":"Data Intelligence New","summary":"更新后的摘要","iconMediaId":%d,"visible":false}
                                """.formatted(version, newMedia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/research-directions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(), mediaAssetMapper.selectById(oldMedia).getStatus());
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(), mediaAssetMapper.selectById(newMedia).getStatus());
    }

    @Test
    void deleteDirection_shouldUnbindMedia() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "delete.png");
        Long directionId = createDirection(session, mediaId, "数据智能", "Data Intelligence", "A", true);
        int version = currentVersion(directionId);

        mockMvc.perform(delete("/admin/api/research-directions/{id}", directionId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(), mediaAssetMapper.selectById(mediaId).getStatus());
        Assertions.assertEquals(0L, researchDirectionMapper.selectCount(null));
    }

    @Test
    void reorderDirection_shouldPersistRequestedOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long first = createDirection(session, uploadImage(session, "a.png"), "数据智能", "Data Intelligence", "A", true);
        Long second = createDirection(session, uploadImage(session, "b.png"), "知识工程", "Knowledge Engineering", "B", true);

        mockMvc.perform(post("/admin/api/research-directions/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedIds":[%d,%d]}
                                """.formatted(second, first)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/research-directions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].titleCn").value("知识工程"));
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

    private Long createDirection(MockHttpSession session, Long mediaId, String titleCn, String titleEn, String summary, boolean visible) throws Exception {
        mockMvc.perform(post("/admin/api/research-directions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"titleCn":"%s","titleEn":"%s","summary":"%s","iconMediaId":%d,"visible":%s}
                                """.formatted(titleCn, titleEn, summary, mediaId, visible)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cms_research_direction WHERE title_cn = ? AND deleted_marker = 0",
                Long.class,
                titleCn);
    }

    private int currentVersion(Long directionId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_research_direction WHERE id = ?",
                Integer.class,
                directionId);
        return version == null ? 0 : version;
    }
}
