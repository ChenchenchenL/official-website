package com.company.officialwebsite.modules.site.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
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
 * NavigationMenuControllerTest：验证导航菜单后台维护与前台暴露的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class NavigationMenuControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cms_navigation_menu");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete("official:portal:navigation");
    }

    @Test
    void adminGetMenus_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/site/navigation/menus"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void createAndPortalGet_shouldReturnVisibleTree_whenConfigurationIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long rootId = createMenu(session, buildCreateJson(0, "关于我们", "GROUP", null, null, null, false, true));
        createMenu(session, buildCreateJson(rootId, "企业介绍", "INTERNAL_ROUTE", "/about", null, null, false, true));
        createMenu(session, buildCreateJson(rootId, "联系我们", "PAGE_ANCHOR", null, "contact-us", null, false, true));

        mockMvc.perform(get("/portal/api/site/navigation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[0].menuName").value("关于我们"))
                .andExpect(jsonPath("$.data[0].children[0].routePath").value("/about"))
                .andExpect(jsonPath("$.data[0].children[1].anchorCode").value("contact-us"));
    }

    @Test
    void createMenu_shouldRejectDuplicateSiblingName() throws Exception {
        MockHttpSession session = loginAsAdmin();
        createMenu(session, buildCreateJson(0, "首页", "INTERNAL_ROUTE", "/home", null, null, false, true));

        mockMvc.perform(post("/admin/api/site/navigation/menus")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateJson(0, "首页", "INTERNAL_ROUTE", "/home-2", null, null, false, true)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_NAVIGATION_NAME_DUPLICATE));
    }

    @Test
    void createMenu_shouldRejectInvalidExternalLinkScheme() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/navigation/menus")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateJson(0, "下载", "EXTERNAL_LINK", null, null, "javascript:alert(1)", true, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_NAVIGATION_TARGET_INVALID));
    }

    @Test
    void updateMenu_shouldRejectNonGroupRoot_whenChildrenExist() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long rootId = createMenu(session, buildCreateJson(0, "产品矩阵", "GROUP", null, null, null, false, true));
        createMenu(session, buildCreateJson(rootId, "ERP", "INTERNAL_ROUTE", "/products/erp", null, null, false, true));
        int version = currentVersion(rootId);

        mockMvc.perform(put("/admin/api/site/navigation/menus/{menuId}", rootId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildUpdateJson(version, "产品矩阵", "INTERNAL_ROUTE", "/products", null, null, false, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_NAVIGATION_TARGET_INVALID));
    }

    @Test
    void deleteRoot_shouldCascadeDeleteChildren() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long rootId = createMenu(session, buildCreateJson(0, "解决方案", "GROUP", null, null, null, false, true));
        createMenu(session, buildCreateJson(rootId, "制造业", "INTERNAL_ROUTE", "/solutions/manufacturing", null, null, false, true));
        createMenu(session, buildCreateJson(rootId, "零售业", "INTERNAL_ROUTE", "/solutions/retail", null, null, false, true));

        mockMvc.perform(delete("/admin/api/site/navigation/menus/{menuId}", rootId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data").isEmpty());

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_navigation_menu WHERE deleted_marker = 0",
                Integer.class);
        Assertions.assertEquals(0, activeCount);
    }

    @Test
    void hideRoot_shouldExcludeWholeTreeFromPortal_withoutOverwritingChildrenVisibility() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long rootId = createMenu(session, buildCreateJson(0, "行业方案", "GROUP", null, null, null, false, true));
        long childId = createMenu(session, buildCreateJson(rootId, "建材行业", "INTERNAL_ROUTE", "/solutions/building", null, null, false, true));
        int rootVersion = currentVersion(rootId);

        mockMvc.perform(put("/admin/api/site/navigation/menus/{menuId}/visibility", rootId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":%d,"visible":false}
                                """.formatted(rootVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/site/navigation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());

        Boolean childVisible = jdbcTemplate.queryForObject(
                "SELECT visible FROM cms_navigation_menu WHERE id = ?",
                Boolean.class,
                childId);
        Assertions.assertTrue(Boolean.TRUE.equals(childVisible));
    }

    @Test
    void reorderMenus_shouldRejectIncompleteSiblingSet() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long rootId = createMenu(session, buildCreateJson(0, "能力中心", "GROUP", null, null, null, false, true));
        long childOne = createMenu(session, buildCreateJson(rootId, "数据平台", "INTERNAL_ROUTE", "/capability/data", null, null, false, true));
        createMenu(session, buildCreateJson(rootId, "智能分析", "INTERNAL_ROUTE", "/capability/analytics", null, null, false, true));

        mockMvc.perform(put("/admin/api/site/navigation/menus/order")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId":%d,"orderedMenuIds":[%d]}
                                """.formatted(rootId, childOne)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void reorderMenus_shouldRejectDuplicateMenuIds() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long rootId = createMenu(session, buildCreateJson(0, "产品能力", "GROUP", null, null, null, false, true));
        long childOne = createMenu(session, buildCreateJson(rootId, "主数据", "INTERNAL_ROUTE", "/product/master", null, null, false, true));
        long childTwo = createMenu(session, buildCreateJson(rootId, "智能决策", "INTERNAL_ROUTE", "/product/ai", null, null, false, true));

        mockMvc.perform(put("/admin/api/site/navigation/menus/order")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"parentId":%d,"orderedMenuIds":[%d,%d,%d]}
                                """.formatted(rootId, childOne, childOne, childTwo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void createMenu_shouldValidateFieldLengths() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/navigation/menus")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(buildCreateJson(0, "M".repeat(65), "INTERNAL_ROUTE", "/home", null, null, false, true)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID))
                .andExpect(jsonPath("$.data.fieldErrors[*].field").value(hasItems("menuName")));
    }

    private long createMenu(MockHttpSession session, String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/api/site/navigation/menus")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM cms_navigation_menu ORDER BY id DESC LIMIT 1",
                Long.class);
        Assertions.assertEquals(TestConstants.SUCCESS, root.path("code").asInt());
        Assertions.assertNotNull(id);
        return id;
    }

    private int currentVersion(long menuId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_navigation_menu WHERE id = ?",
                Integer.class,
                menuId);
        return version == null ? 0 : version;
    }

    private String buildCreateJson(
            long parentId,
            String menuName,
            String targetType,
            String routePath,
            String anchorCode,
            String externalUrl,
            boolean openInNewTab,
            boolean visible) {
        return """
                {
                  "parentId":%d,
                  "menuName":%s,
                  "targetType":%s,
                  "routePath":%s,
                  "anchorCode":%s,
                  "externalUrl":%s,
                  "openInNewTab":%s,
                  "visible":%s
                }
                """.formatted(
                parentId,
                toJsonValue(menuName),
                toJsonValue(targetType),
                toJsonValue(routePath),
                toJsonValue(anchorCode),
                toJsonValue(externalUrl),
                openInNewTab,
                visible);
    }

    private String buildUpdateJson(
            int version,
            String menuName,
            String targetType,
            String routePath,
            String anchorCode,
            String externalUrl,
            boolean openInNewTab,
            boolean visible) {
        return """
                {
                  "version":%d,
                  "menuName":%s,
                  "targetType":%s,
                  "routePath":%s,
                  "anchorCode":%s,
                  "externalUrl":%s,
                  "openInNewTab":%s,
                  "visible":%s
                }
                """.formatted(
                version,
                toJsonValue(menuName),
                toJsonValue(targetType),
                toJsonValue(routePath),
                toJsonValue(anchorCode),
                toJsonValue(externalUrl),
                openInNewTab,
                visible);
    }

    private String toJsonValue(String value) {
        if (value == null) {
            return "null";
        }
        return '"' + value.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
    }
}
