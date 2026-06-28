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

import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.site.mapper.ValueCardMapper;
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
 * ValueCardControllerTest：验证核心价值观卡片后台维护与前台暴露的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ValueCardControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ValueCardMapper valueCardMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE biz_field = 'icon' AND biz_module = 'SITE'");
        jdbcTemplate.update("DELETE FROM cms_value_card WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        jdbcTemplate.update(
                "UPDATE cms_value_card SET visible = 1, deleted_marker = 0, icon_media_id = -1 WHERE id IN (-9401, -9402, -9403)");
        jdbcTemplate.update(
                "UPDATE cms_value_card SET sort_order = CASE id WHEN -9401 THEN 10 WHEN -9402 THEN 20 WHEN -9403 THEN 30 END, title = CASE id WHEN -9401 THEN '同事' WHEN -9402 THEN '同仁' WHEN -9403 THEN '同享' END WHERE id IN (-9401, -9402, -9403)");
        redisTemplate.delete(portalCacheKeyBuilder.build("value_cards"));
    }

    @Test
    void adminGetValueCards_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/value-cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void portalGetValueCards_shouldReturnDefaultSeedCardsInOrder() throws Exception {
        mockMvc.perform(get("/portal/api/value-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].title").value("同事"))
                .andExpect(jsonPath("$.data[1].title").value("同仁"))
                .andExpect(jsonPath("$.data[2].title").value("同享"));
    }

    @Test
    void createValueCard_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = uploadImage(session, "icon.png");

        mockMvc.perform(post("/admin/api/value-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iconMediaId":%d,"title":"同创","subtitle":"共开新局","description":"携手开拓新业务方向。","visible":true}
                                """.formatted(iconId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/value-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title").value(hasItems("同创")));
    }

    @Test
    void createValueCard_shouldRejectDuplicateTitle() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = uploadImage(session, "icon-dup.png");

        mockMvc.perform(post("/admin/api/value-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iconMediaId":%d,"title":"同事","subtitle":"重复标题测试","description":"重复标题描述。","visible":true}
                                """.formatted(iconId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_VALUE_CARD_TITLE_DUPLICATE));
    }

    @Test
    void createValueCard_shouldRejectInvalidIconMediaId() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/value-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iconMediaId":999999,"title":"新卡片","subtitle":"无效图标测试","description":"图标不存在。","visible":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_VALUE_CARD_ICON_INVALID));
    }

    @Test
    void updateValueCard_shouldRejectVersionConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = uploadImage(session, "icon-conflict.png");
        Long cardId = createValueCard(session, iconId, "并发卡片", "并发副标语", "并发描述。", true);

        mockMvc.perform(put("/admin/api/value-cards/{id}", cardId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":999,"iconMediaId":%d,"title":"并发卡片-新版","subtitle":"副标语更新","description":"描述更新。","visible":true}
                                """.formatted(iconId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void deleteValueCard_shouldLogicallyDeleteAndAllowRecreation() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = uploadImage(session, "icon-del.png");
        Long cardId = createValueCard(session, iconId, "待删除卡片", "待删除副标语", "删除测试。", true);
        int version = currentVersion(cardId);

        mockMvc.perform(delete("/admin/api/value-cards/{id}", cardId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_value_card WHERE id = ? AND deleted_marker = 0",
                Integer.class, cardId);
        Assertions.assertEquals(0, activeCount == null ? 0 : activeCount);

        mockMvc.perform(post("/admin/api/value-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iconMediaId":%d,"title":"待删除卡片","subtitle":"重建副标语","description":"删除后重建测试。","visible":true}
                                """.formatted(iconId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
    }

    @Test
    void reorderValueCards_shouldPersistRequestedOrder() throws Exception {
        jdbcTemplate.update("UPDATE cms_value_card SET deleted_marker = id WHERE id IN (-9401, -9402, -9403)");
        MockHttpSession session = loginAsAdmin();
        Long iconId1 = uploadImage(session, "icon-r1.png");
        Long iconId2 = uploadImage(session, "icon-r2.png");
        Long id1 = createValueCard(session, iconId1, "卡片甲", "甲副标语", "甲描述。", true);
        Long id2 = createValueCard(session, iconId2, "卡片乙", "乙副标语", "乙描述。", true);

        mockMvc.perform(post("/admin/api/value-cards/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedValueCardIds":[%d,%d]}
                                """.formatted(id2, id1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer sortOrder1 = jdbcTemplate.queryForObject(
                "SELECT sort_order FROM cms_value_card WHERE id = ?", Integer.class, id1);
        Integer sortOrder2 = jdbcTemplate.queryForObject(
                "SELECT sort_order FROM cms_value_card WHERE id = ?", Integer.class, id2);
        Assertions.assertTrue(sortOrder2 < sortOrder1, "重排后卡片乙应排在卡片甲前面");
    }

    @Test
    void reorderValueCards_shouldRejectIncompleteSet() throws Exception {
        jdbcTemplate.update("UPDATE cms_value_card SET deleted_marker = id WHERE id IN (-9401, -9402, -9403)");
        MockHttpSession session = loginAsAdmin();
        Long iconId = uploadImage(session, "icon-inc.png");
        Long id1 = createValueCard(session, iconId, "卡片A", "A副标语", "A描述。", true);
        createValueCard(session, iconId, "卡片B", "B副标语", "B描述。", true);

        mockMvc.perform(post("/admin/api/value-cards/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedValueCardIds":[%d]}
                                """.formatted(id1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void portalGetValueCards_shouldRespectVisibility() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = uploadImage(session, "icon-hidden.png");
        createValueCard(session, iconId, "隐藏卡片", "隐藏副标语", "不可见。", false);

        mockMvc.perform(get("/portal/api/value-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title").value(org.hamcrest.Matchers.not(hasItems("隐藏卡片"))));
    }

    @Test
    void portalGetValueCards_shouldReturnIconUrl() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = uploadImage(session, "icon-url.png");
        createValueCard(session, iconId, "带图标卡片", "图标副标语", "图标描述。", true);

        mockMvc.perform(get("/portal/api/value-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.title=='带图标卡片')].iconUrl").isNotEmpty());
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

    private Long createValueCard(MockHttpSession session, Long iconMediaId, String title, String subtitle,
            String description, boolean visible) throws Exception {
        mockMvc.perform(post("/admin/api/value-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iconMediaId":%d,"title":"%s","subtitle":"%s","description":"%s","visible":%s}
                                """.formatted(iconMediaId, title, subtitle, description, visible)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cms_value_card WHERE title = ? AND deleted_marker = 0",
                Long.class, title);
    }

    private int currentVersion(Long cardId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_value_card WHERE id = ?", Integer.class, cardId);
        return version == null ? 0 : version;
    }
}
