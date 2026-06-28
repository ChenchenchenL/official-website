package com.company.officialwebsite.modules.lead.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
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

/**
 * CooperationDirectionTagControllerTest：验证合作方向标签后台维护、前台读取、重排、缓存与审计的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CooperationDirectionTagControllerTest extends BaseAdminControllerIntegrationTest {

    private static final String CACHE_SEGMENT = "cooperation_direction_tags";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cms_cooperation_direction_tag WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_SEGMENT));
    }

    @Test
    void adminGetTags_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/cooperation-direction-tags"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void adminGetTags_shouldReturnEmptyList_whenNoTagsExist() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/cooperation-direction-tags").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void portalGetTags_shouldReturnEmptyArray_whenNoTagsExist() throws Exception {
        mockMvc.perform(get("/portal/api/cooperation-direction-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void createTag_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"企业数字化建设"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/cooperation-direction-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].tagText").value("企业数字化建设"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].version").doesNotExist());
    }

    @Test
    void createTag_shouldRejectDuplicateTagText() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"企业数字化建设"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"企业数字化建设"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.LEAD_COOPERATION_DIRECTION_TAG_TEXT_DUPLICATE));
    }

    @Test
    void createTag_shouldRejectBlankTagText() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void createTag_shouldAppendToEndOfSortOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"标签甲"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"标签乙"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/portal/api/cooperation-direction-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tagText").value("标签甲"))
                .andExpect(jsonPath("$.data[1].tagText").value("标签乙"));
    }

    @Test
    void createTag_shouldAllowRecreationAfterDelete() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"待删除标签"}
                                """))
                .andExpect(status().isOk());
        Long tagId = queryActiveTagId("待删除标签");
        int version = currentVersion(tagId);

        mockMvc.perform(delete("/admin/api/cooperation-direction-tags/{id}", tagId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"待删除标签"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
    }

    @Test
    void updateTag_shouldRejectDuplicateTagText() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"标签甲"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"标签乙"}
                                """))
                .andExpect(status().isOk());
        Long tagId = queryActiveTagId("标签乙");

        mockMvc.perform(put("/admin/api/cooperation-direction-tags/{id}", tagId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"tagText":"标签甲"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.LEAD_COOPERATION_DIRECTION_TAG_TEXT_DUPLICATE));
    }

    @Test
    void updateTag_shouldRejectVersionConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"并发标签"}
                                """))
                .andExpect(status().isOk());
        Long tagId = queryActiveTagId("并发标签");

        mockMvc.perform(put("/admin/api/cooperation-direction-tags/{id}", tagId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":999,"tagText":"并发标签-新版"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void deleteTag_shouldReturn70002_whenTagNotExists() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(delete("/admin/api/cooperation-direction-tags/{id}", 999999L)
                        .session(session)
                        .with(csrf())
                        .param("version", "0"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(TestConstants.LEAD_COOPERATION_DIRECTION_TAG_NOT_FOUND));
    }

    @Test
    void deleteTag_shouldHideFromPortal() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"临时标签"}
                                """))
                .andExpect(status().isOk());
        Long tagId = queryActiveTagId("临时标签");
        int version = currentVersion(tagId);

        mockMvc.perform(get("/portal/api/cooperation-direction-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].tagText").value(hasItems("临时标签")));

        mockMvc.perform(delete("/admin/api/cooperation-direction-tags/{id}", tagId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/cooperation-direction-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].tagText").value(
                        org.hamcrest.Matchers.not(hasItems("临时标签"))));
    }

    @Test
    void portalGetTags_shouldReturnInSortOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long id1 = createTag(session, "标签A");
        Long id2 = createTag(session, "标签B");
        Long id3 = createTag(session, "标签C");

        mockMvc.perform(post("/admin/api/cooperation-direction-tags/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedCooperationDirectionTagIds":[%d,%d,%d]}
                                """.formatted(id3, id1, id2)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/portal/api/cooperation-direction-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tagText").value("标签C"))
                .andExpect(jsonPath("$.data[1].tagText").value("标签A"))
                .andExpect(jsonPath("$.data[2].tagText").value("标签B"));
    }

    @Test
    void reorderTags_shouldRejectIncompleteSet() throws Exception {
        MockHttpSession session = loginAsAdmin();
        createTag(session, "标签A");
        createTag(session, "标签B");

        Long idA = queryActiveTagId("标签A");

        mockMvc.perform(post("/admin/api/cooperation-direction-tags/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedCooperationDirectionTagIds":[%d]}
                                """.formatted(idA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void reorderTags_shouldRejectDuplicateIds() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long id1 = createTag(session, "标签A");

        mockMvc.perform(post("/admin/api/cooperation-direction-tags/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedCooperationDirectionTagIds":[%d,%d]}
                                """.formatted(id1, id1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void reorderTags_shouldReturnParamInvalid_whenNoActiveTags() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedCooperationDirectionTagIds":[1]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void reorderTags_shouldRejectIdsContainingDeletedTags() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long id1 = createTag(session, "标签A");
        Long id2 = createTag(session, "标签B");
        int version = currentVersion(id1);

        mockMvc.perform(delete("/admin/api/cooperation-direction-tags/{id}", id1)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/api/cooperation-direction-tags/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedCooperationDirectionTagIds":[%d,%d]}
                                """.formatted(id1, id2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void writeOperations_shouldInvalidatePortalCache() throws Exception {
        MockHttpSession session = loginAsAdmin();
        String cacheKey = portalCacheKeyBuilder.build(CACHE_SEGMENT);

        createTag(session, "缓存标签");

        mockMvc.perform(get("/portal/api/cooperation-direction-tags"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].tagText").value(hasItems("缓存标签")));
        Thread.sleep(1200L);

        mockMvc.perform(get("/portal/api/cooperation-direction-tags"))
                .andExpect(status().isOk());
        Assertions.assertNotNull(redisTemplate.opsForValue().get(cacheKey),
                "Portal 缓存应在首次读取后重建");

        Long tagId = queryActiveTagId("缓存标签");
        mockMvc.perform(put("/admin/api/cooperation-direction-tags/{id}", tagId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"tagText":"缓存标签-已更新"}
                                """))
                .andExpect(status().isOk());

        Thread.sleep(200L);
        Assertions.assertNull(redisTemplate.opsForValue().get(cacheKey),
                "更新后 Portal 缓存应被失效");
    }

    @Test
    void allWriteOperations_shouldWriteAuditLog() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"审计标签"}
                                """))
                .andExpect(status().isOk());

        assertAuditRecordExists("CREATE_COOPERATION_DIRECTION_TAG");

        Long tagId = queryActiveTagId("审计标签");
        mockMvc.perform(put("/admin/api/cooperation-direction-tags/{id}", tagId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"tagText":"审计标签-更新"}
                                """))
                .andExpect(status().isOk());

        assertAuditRecordExists("UPDATE_COOPERATION_DIRECTION_TAG");

        Long tag2Id = createTag(session, "审计标签2");
        int version = currentVersion(tag2Id);
        mockMvc.perform(delete("/admin/api/cooperation-direction-tags/{id}", tag2Id)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk());

        assertAuditRecordExists("DELETE_COOPERATION_DIRECTION_TAG");

        Long tag3Id = createTag(session, "审计标签3");
        Long tag4Id = createTag(session, "审计标签4");
        mockMvc.perform(post("/admin/api/cooperation-direction-tags/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedCooperationDirectionTagIds":[%d,%d,%d]}
                                """.formatted(tagId, tag4Id, tag3Id)))
                .andExpect(status().isOk());

        assertAuditRecordExists("REORDER_COOPERATION_DIRECTION_TAG");
    }

    private Long createTag(MockHttpSession session, String tagText) throws Exception {
        mockMvc.perform(post("/admin/api/cooperation-direction-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"%s"}
                                """.formatted(tagText)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        return queryActiveTagId(tagText);
    }

    private Long queryActiveTagId(String tagText) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cms_cooperation_direction_tag WHERE tag_text = ? AND deleted_marker = 0",
                Long.class, tagText);
    }

    private int currentVersion(Long tagId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_cooperation_direction_tag WHERE id = ?", Integer.class, tagId);
        return version == null ? 0 : version;
    }

    private void assertAuditRecordExists(String actionName) {
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE module_name = 'LEAD' "
                        + "AND action_name = ? AND target_type = 'COOPERATION_DIRECTION_TAG'",
                Integer.class, actionName);
        Assertions.assertNotNull(auditCount);
        Assertions.assertTrue(auditCount > 0,
                "审计日志应存在 %s 记录".formatted(actionName));
    }
}
