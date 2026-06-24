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
import org.springframework.test.web.servlet.MvcResult;

/**
 * HomeMetricCardControllerTest：验证首页核心数据指标卡片后台维护与前台暴露的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class HomeMetricCardControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cms_home_metric_card");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build("home", "metrics"));
    }

    @Test
    void adminGetMetricCards_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/site/home-metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void createAndPortalGet_shouldReturnVisibleMetricCards_whenConfigurationIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();

        createMetricCard(session, """
                {"value":"11","unit":"+年","description":"行业数字化实践经验","visible":true}
                """);
        createMetricCard(session, """
                {"value":"100","unit":"+","description":"服务客户数量","visible":false}
                """);

        mockMvc.perform(get("/portal/api/site/home-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[0].value").value("11"))
                .andExpect(jsonPath("$.data[0].unit").value("+年"))
                .andExpect(jsonPath("$.data[0].description").value("行业数字化实践经验"))
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void createMetricCard_shouldRejectInvalidNumericValue() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/home-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":"11+","unit":"年","description":"行业数字化实践经验","visible":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_HOME_METRIC_VALUE_INVALID));
    }

    @Test
    void createMetricCard_shouldRejectTooManyIntegerDigits() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/home-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":"1234567890123","unit":"年","description":"行业数字化实践经验","visible":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_HOME_METRIC_VALUE_INVALID));
    }

    @Test
    void createMetricCard_shouldValidateFieldLengths() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/home-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"value":"1","unit":"%s","description":"%s","visible":true}
                                """.formatted("U".repeat(33), "D".repeat(121))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID))
                .andExpect(jsonPath("$.data.fieldErrors[*].field").value(hasItems("unit", "description")));
    }

    @Test
    void updateMetricCard_shouldRejectStaleVersion() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long metricId = createMetricCard(session, """
                {"value":"11","unit":"+年","description":"行业数字化实践经验","visible":true}
                """);

        mockMvc.perform(put("/admin/api/site/home-metrics/{metricId}", metricId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"value":"12","unit":"+年","description":"更新后的文案"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(put("/admin/api/site/home-metrics/{metricId}", metricId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"value":"13","unit":"+年","description":"再次更新"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void updateVisibility_shouldExcludeMetricCardFromPortal_whenHidden() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long metricId = createMetricCard(session, """
                {"value":"11","unit":"+年","description":"行业数字化实践经验","visible":true}
                """);
        int version = currentVersion(metricId);

        mockMvc.perform(put("/admin/api/site/home-metrics/{metricId}/visibility", metricId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":%d,"visible":false}
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/site/home-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void deleteMetricCard_shouldRemoveMetricCardFromActiveList() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long metricId = createMetricCard(session, """
                {"value":"11","unit":"+年","description":"行业数字化实践经验","visible":true}
                """);

        mockMvc.perform(delete("/admin/api/site/home-metrics/{metricId}", metricId)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data").isEmpty());

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_home_metric_card WHERE deleted_marker = 0",
                Integer.class);
        Assertions.assertEquals(0, activeCount);
    }

    @Test
    void reorderMetricCards_shouldRejectIncompleteSet() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long firstId = createMetricCard(session, """
                {"value":"11","unit":"+年","description":"行业数字化实践经验","visible":true}
                """);
        createMetricCard(session, """
                {"value":"100","unit":"+","description":"服务客户数量","visible":true}
                """);

        mockMvc.perform(put("/admin/api/site/home-metrics/order")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedMetricIds":[%d]}
                                """.formatted(firstId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void reorderMetricCards_shouldPersistRequestedOrderAndWriteAudit() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long firstId = createMetricCard(session, """
                {"value":"11","unit":"+年","description":"行业数字化实践经验","visible":true}
                """);
        long secondId = createMetricCard(session, """
                {"value":"100","unit":"+","description":"服务客户数量","visible":true}
                """);

        mockMvc.perform(put("/admin/api/site/home-metrics/order")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedMetricIds":[%d,%d]}
                                """.formatted(secondId, firstId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[0].id").value(secondId))
                .andExpect(jsonPath("$.data[1].id").value(firstId));

        mockMvc.perform(get("/portal/api/site/home-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").doesNotExist())
                .andExpect(jsonPath("$.data[0].value").value("100"))
                .andExpect(jsonPath("$.data[1].id").doesNotExist())
                .andExpect(jsonPath("$.data[1].value").value("11"));

        Assertions.assertEquals(3L, sysAuditLogMapper.selectCount(null));
    }

    private long createMetricCard(MockHttpSession session, String json) throws Exception {
        MvcResult result = mockMvc.perform(post("/admin/api/site/home-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM cms_home_metric_card ORDER BY id DESC LIMIT 1",
                Long.class);
        Assertions.assertEquals(TestConstants.SUCCESS, root.path("code").asInt());
        Assertions.assertNotNull(id);
        return id;
    }

    private int currentVersion(long metricId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_home_metric_card WHERE id = ?",
                Integer.class,
                metricId);
        return version == null ? 0 : version;
    }
}
