package com.company.officialwebsite.modules.site.controller;

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
import org.springframework.test.web.servlet.MvcResult;

/**
 * PromiseControllerTest：验证"我们的承诺"主体文案与承诺标签后台维护、前台聚合输出的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class PromiseControllerTest extends BaseAdminControllerIntegrationTest {

    private static final String CACHE_SEGMENT = "our_promises";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cms_promise_tag WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        jdbcTemplate.update(
                "UPDATE cms_promise_content SET content = '云台数据全体员工将继续秉承专业、务实、创新、共赢的理念，为客户持续创造真实价值。', version = 0, deleted_marker = 0 WHERE config_key = 'default' AND id > 0");
        jdbcTemplate.update(
                "INSERT INTO cms_promise_content (config_key, content, version, deleted_marker) SELECT 'default', '云台数据全体员工将继续秉承专业、务实、创新、共赢的理念，为客户持续创造真实价值。', 0, 0 WHERE NOT EXISTS (SELECT 1 FROM cms_promise_content WHERE config_key = 'default' AND deleted_marker = 0)");
        jdbcTemplate.update(
                "UPDATE cms_promise_tag SET deleted_marker = 0, tag_text = CASE id WHEN -9501 THEN '过硬的技术' WHEN -9502 THEN '简便的操作' WHEN -9503 THEN '实用的功能' END, sort_order = CASE id WHEN -9501 THEN 10 WHEN -9502 THEN 20 WHEN -9503 THEN 30 END WHERE id IN (-9501, -9502, -9503)");
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_SEGMENT));
    }

    @Test
    void adminGetPromiseContent_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/promise-content"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void adminGetPromiseContent_shouldReturnDefaultContent() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/promise-content").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.content").isNotEmpty())
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    void updatePromiseContent_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        String cacheKey = portalCacheKeyBuilder.build(CACHE_SEGMENT);
        stabilizePortalCacheKey(cacheKey);

        mockMvc.perform(get("/portal/api/our-promises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        Assertions.assertNotNull(redisTemplate.opsForValue().get(cacheKey));

        mockMvc.perform(put("/admin/api/promise-content")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"content":"更新后的主体宣导文案。"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertNull(redisTemplate.opsForValue().get(cacheKey));

        mockMvc.perform(get("/portal/api/our-promises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("更新后的主体宣导文案。"));
    }

    @Test
    void updatePromiseContent_shouldRejectVersionConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/promise-content")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":999,"content":"并发更新文案。"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void portalGetPromiseModule_shouldReturnDefaultTagsInOrder() throws Exception {
        mockMvc.perform(get("/portal/api/our-promises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.tags.length()").value(3))
                .andExpect(jsonPath("$.data.tags[0].tagText").value("过硬的技术"))
                .andExpect(jsonPath("$.data.tags[1].tagText").value("简便的操作"))
                .andExpect(jsonPath("$.data.tags[2].tagText").value("实用的功能"));
    }

    @Test
    void createPromiseTag_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        String cacheKey = portalCacheKeyBuilder.build(CACHE_SEGMENT);
        stabilizePortalCacheKey(cacheKey);

        mockMvc.perform(get("/portal/api/our-promises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        Assertions.assertNotNull(redisTemplate.opsForValue().get(cacheKey));

        mockMvc.perform(post("/admin/api/promise-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"务实的交付"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertNull(redisTemplate.opsForValue().get(cacheKey));

        mockMvc.perform(get("/portal/api/our-promises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags[*].tagText").value(hasItems("务实的交付")));
    }

    @Test
    void createPromiseTag_shouldRejectDuplicateTagText() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/promise-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"过硬的技术"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_PROMISE_TAG_TEXT_DUPLICATE));
    }

    @Test
    void updatePromiseTag_shouldRejectVersionConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long tagId = createPromiseTag(session, "并发标签");

        mockMvc.perform(put("/admin/api/promise-tags/{id}", tagId)
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
    void deletePromiseTag_shouldLogicallyDeleteAndAllowRecreation() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long tagId = createPromiseTag(session, "待删除标签");
        int version = currentVersion(tagId);

        mockMvc.perform(delete("/admin/api/promise-tags/{id}", tagId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_promise_tag WHERE id = ? AND deleted_marker = 0",
                Integer.class, tagId);
        Assertions.assertEquals(0, activeCount == null ? 0 : activeCount);

        mockMvc.perform(post("/admin/api/promise-tags")
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
    void reorderPromiseTags_shouldPersistRequestedOrder() throws Exception {
        jdbcTemplate.update("UPDATE cms_promise_tag SET deleted_marker = id WHERE id IN (-9501, -9502, -9503)");
        MockHttpSession session = loginAsAdmin();
        Long id1 = createPromiseTag(session, "标签甲");
        Long id2 = createPromiseTag(session, "标签乙");

        mockMvc.perform(post("/admin/api/promise-tags/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedPromiseTagIds":[%d,%d]}
                                """.formatted(id2, id1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer sortOrder1 = jdbcTemplate.queryForObject(
                "SELECT sort_order FROM cms_promise_tag WHERE id = ?", Integer.class, id1);
        Integer sortOrder2 = jdbcTemplate.queryForObject(
                "SELECT sort_order FROM cms_promise_tag WHERE id = ?", Integer.class, id2);
        Assertions.assertTrue(sortOrder2 < sortOrder1, "重排后标签乙应排在标签甲前面");
    }

    @Test
    void reorderPromiseTags_shouldRejectIncompleteSet() throws Exception {
        jdbcTemplate.update("UPDATE cms_promise_tag SET deleted_marker = id WHERE id IN (-9501, -9502, -9503)");
        MockHttpSession session = loginAsAdmin();
        Long id1 = createPromiseTag(session, "标签A");
        createPromiseTag(session, "标签B");

        mockMvc.perform(post("/admin/api/promise-tags/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedPromiseTagIds":[%d]}
                                """.formatted(id1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void portalGetPromiseModule_shouldReflectTagVisibilityAfterDelete() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long tagId = createPromiseTag(session, "临时标签");
        int version = currentVersion(tagId);

        mockMvc.perform(get("/portal/api/our-promises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags[*].tagText").value(hasItems("临时标签")));

        mockMvc.perform(delete("/admin/api/promise-tags/{id}", tagId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/our-promises"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tags[*].tagText").value(
                        org.hamcrest.Matchers.not(hasItems("临时标签"))));
    }

    private Long createPromiseTag(MockHttpSession session, String tagText) throws Exception {
        mockMvc.perform(post("/admin/api/promise-tags")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tagText":"%s"}
                                """.formatted(tagText)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cms_promise_tag WHERE tag_text = ? AND deleted_marker = 0",
                Long.class, tagText);
    }

    private int currentVersion(Long tagId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_promise_tag WHERE id = ?", Integer.class, tagId);
        return version == null ? 0 : version;
    }

    /**
     * Promise Portal 缓存采用提交后删除加延迟二次删除策略，先等待上个用例遗留任务完成，
     * 避免误删当前用例刚重建的缓存导致断言抖动。
     */
    private void stabilizePortalCacheKey(String cacheKey) throws InterruptedException {
        Thread.sleep(1200L);
        redisTemplate.delete(cacheKey);
    }
}
