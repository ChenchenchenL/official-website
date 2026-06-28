package com.company.officialwebsite.modules.lead.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.officialwebsite.modules.lead.service.LeadModuleConstants;
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
 * LeadControllerTest：验证前台匿名提交、后台分页/详情/状态流转/导出/限流/审计的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class LeadControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cms_lead_record WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(LeadModuleConstants.RATE_LIMIT_KEY_PREFIX + "127.0.0.1");
        redisTemplate.delete(LeadModuleConstants.RATE_LIMIT_KEY_PREFIX + "0:0:0:0:0:0:0:1");
    }

    @Test
    void portalSubmit_shouldPersist_whenRequestIsValid() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"示例公司","email":"zhangsan@example.com",
                                 "phone":"+86 027-88886666","demandDescription":"希望了解企业数字化方案"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_lead_record WHERE name = '张三' AND deleted_marker = 0",
                Integer.class);
        Assertions.assertNotNull(count);
        Assertions.assertEquals(1, count);

        Integer statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM cms_lead_record WHERE name = '张三' AND deleted_marker = 0",
                Integer.class);
        Assertions.assertEquals(0, statusValue, "新建线索默认状态应为 UNHANDLED(0)");
    }

    @Test
    void portalSubmit_shouldRejectBlankName() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","company":"示例公司","email":"zhangsan@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void portalSubmit_shouldRejectBlankCompany() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"","email":"zhangsan@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void portalSubmit_shouldRejectInvalidEmail() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"示例公司","email":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void portalSubmit_shouldRejectPhoneWithIllegalChars() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"示例公司","email":"zhangsan@example.com",
                                 "phone":"abc-defg"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void portalSubmit_shouldRejectPhoneWithoutDigit() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"示例公司","email":"zhangsan@example.com",
                                 "phone":"+--()/#"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void portalSubmit_shouldRejectOversizedDemandDescription() throws Exception {
        String longDescription = "x".repeat(1001);
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"示例公司","email":"zhangsan@example.com",
                                 "demandDescription":"%s"}
                                """.formatted(longDescription)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void portalSubmit_shouldAcceptAllowedPhoneChars() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"示例公司","email":"zhangsan@example.com",
                                 "phone":"+86 (027) 8888-6666/123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
    }

    @Test
    void portalSubmit_shouldAcceptOptionalPhoneAndDescription() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"示例公司","email":"zhangsan@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
    }

    @Test
    void portalSubmit_shouldTriggerRateLimitAfterFiveSubmissions() throws Exception {
        for (int i = 1; i <= 5; i++) {
            mockMvc.perform(post("/portal/api/leads")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"用户%d","company":"示例公司","email":"user%d@example.com"}
                                    """.formatted(i, i)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        }

        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"限流用户","company":"示例公司","email":"limited@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.LEAD_SUBMIT_RATE_LIMITED));
    }

    @Test
    void adminGetLeadPage_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/leads"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void adminGetLeadPage_shouldReturnMaskedFields() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", "+86 027-88886666", "希望了解企业数字化方案");
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(get("/admin/api/leads").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.list[0].name").value("张三"))
                .andExpect(jsonPath("$.data.list[0].maskedEmail").value("z***@example.com"))
                .andExpect(jsonPath("$.data.list[0].maskedPhone").isNotEmpty())
                .andExpect(jsonPath("$.data.list[0].demandDescriptionPreview").value("希望了解企业数字化方案"))
                .andExpect(jsonPath("$.data.list[0].status").value(0))
                .andExpect(jsonPath("$.data.list[0].statusLabel").value("未处理"));
    }

    @Test
    void adminGetLeadPage_shouldFilterByStatus() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", null, null);
        submitLead("李四", "示例公司", "lisi@example.com", null, null);
        MockHttpSession session = loginAsAdmin();

        Long secondId = queryLeadId("李四");
        updateStatus(session, secondId, 1);

        mockMvc.perform(get("/admin/api/leads").session(session)
                        .param("status", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].name").value("李四"));
    }

    @Test
    void adminGetLeadPage_shouldRejectInvalidStatus() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/leads").session(session)
                        .param("status", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.LEAD_STATUS_INVALID));
    }

    @Test
    void adminGetLeadPage_shouldSortByCreatedAtDesc() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", null, null);
        submitLead("李四", "示例公司", "lisi@example.com", null, null);
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(get("/admin/api/leads").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list[0].name").value("李四"))
                .andExpect(jsonPath("$.data.list[1].name").value("张三"));
    }

    @Test
    void adminGetLeadDetail_shouldReturnFullInfo() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", "+86 027-88886666", "需求描述全文");
        MockHttpSession session = loginAsAdmin();
        Long leadId = queryLeadId("张三");

        mockMvc.perform(get("/admin/api/leads/{id}", leadId).session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value("zhangsan@example.com"))
                .andExpect(jsonPath("$.data.phone").value("+86 027-88886666"))
                .andExpect(jsonPath("$.data.demandDescription").value("需求描述全文"))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.submitIp").isNotEmpty());
    }

    @Test
    void adminGetLeadDetail_shouldReturn70004_whenNotExists() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/leads/{id}", 999999L).session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(TestConstants.LEAD_RECORD_NOT_FOUND));
    }

    @Test
    void updateStatus_shouldPersistAndIncrementVersion() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", null, null);
        MockHttpSession session = loginAsAdmin();
        Long leadId = queryLeadId("张三");

        mockMvc.perform(put("/admin/api/leads/{id}/status", leadId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"status":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_lead_record WHERE id = ?", Integer.class, leadId);
        Assertions.assertEquals(1, version, "状态更新后版本号应递增");

        Integer statusValue = jdbcTemplate.queryForObject(
                "SELECT status FROM cms_lead_record WHERE id = ?", Integer.class, leadId);
        Assertions.assertEquals(1, statusValue);
    }

    @Test
    void updateStatus_shouldBeIdempotent_whenSameStatus() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", null, null);
        MockHttpSession session = loginAsAdmin();
        Long leadId = queryLeadId("张三");

        mockMvc.perform(put("/admin/api/leads/{id}/status", leadId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"status":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_lead_record WHERE id = ?", Integer.class, leadId);
        Assertions.assertEquals(0, version, "幂等更新不应改变版本号");
    }

    @Test
    void updateStatus_shouldRejectInvalidStatusValueAtDtoLayer() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", null, null);
        MockHttpSession session = loginAsAdmin();
        Long leadId = queryLeadId("张三");

        mockMvc.perform(put("/admin/api/leads/{id}/status", leadId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                .content("""
                                {"version":0,"status":99}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void updateStatus_shouldRejectVersionConflict() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", null, null);
        MockHttpSession session = loginAsAdmin();
        Long leadId = queryLeadId("张三");

        mockMvc.perform(put("/admin/api/leads/{id}/status", leadId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":999,"status":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void exportLeads_shouldReturnExcel_whenFilteredMode() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", null, null);
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/leads/export")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exportMode":"FILTERED","status":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
    }

    @Test
    void exportLeads_shouldRejectEmptyFilterConditions() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/leads/export")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exportMode":"FILTERED"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void exportLeads_shouldRejectEmptySelectedIds() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/leads/export")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exportMode":"SELECTED","selectedIds":[]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void exportLeads_shouldRejectInvalidExportMode() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/leads/export")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exportMode":"INVALID"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void createLead_shouldMaskSensitiveFieldsInAuditSnapshot() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"示例公司","email":"zhangsan@example.com","phone":"13812345678"}
                                """))
                .andExpect(status().isOk());

        String afterSnapshot = jdbcTemplate.queryForObject(
                "SELECT after_snapshot FROM sys_audit_log WHERE module_name = 'LEAD' AND action_name = 'CREATE_LEAD' ORDER BY id DESC LIMIT 1",
                String.class);
        Assertions.assertNotNull(afterSnapshot);
        Assertions.assertFalse(afterSnapshot.contains("zhangsan@example.com"));
        Assertions.assertFalse(afterSnapshot.contains("13812345678"));
        Assertions.assertTrue(afterSnapshot.contains("z***@example.com"));
        Assertions.assertTrue(afterSnapshot.contains("138"));
    }

    @Test
    void allOperations_shouldWriteAuditLog() throws Exception {
        submitLead("张三", "示例公司", "zhangsan@example.com", null, null);
        Long leadId = queryLeadId("张三");
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(get("/admin/api/leads/{id}", leadId).session(session))
                .andExpect(status().isOk());
        assertAuditRecordExists("VIEW_LEAD_DETAIL");

        mockMvc.perform(put("/admin/api/leads/{id}/status", leadId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"status":1}
                                """))
                .andExpect(status().isOk());
        assertAuditRecordExists("UPDATE_LEAD_STATUS");

        mockMvc.perform(post("/admin/api/leads/export")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"exportMode":"FILTERED","status":1}
                                """))
                .andExpect(status().isOk());
        assertAuditRecordExists("EXPORT_LEAD");
    }

    @Test
    void createLead_shouldWriteAuditLog() throws Exception {
        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"张三","company":"示例公司","email":"zhangsan@example.com"}
                                """))
                .andExpect(status().isOk());

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE module_name = 'LEAD' AND action_name = 'CREATE_LEAD'",
                Integer.class);
        Assertions.assertNotNull(auditCount);
        Assertions.assertTrue(auditCount > 0, "审计日志应存在 CREATE_LEAD 记录");
    }

    private void submitLead(String name, String company, String email, String phone, String demand) throws Exception {
        StringBuilder content = new StringBuilder();
        content.append("{\"name\":\"").append(name)
                .append("\",\"company\":\"").append(company)
                .append("\",\"email\":\"").append(email).append("\"");
        if (phone != null) {
            content.append(",\"phone\":\"").append(phone).append("\"");
        }
        if (demand != null) {
            content.append(",\"demandDescription\":\"").append(demand).append("\"");
        }
        content.append("}");

        mockMvc.perform(post("/portal/api/leads")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content.toString()))
                .andExpect(status().isOk());
    }

    private Long queryLeadId(String name) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cms_lead_record WHERE name = ? AND deleted_marker = 0",
                Long.class, name);
    }

    private void updateStatus(MockHttpSession session, Long leadId, int status) throws Exception {
        mockMvc.perform(put("/admin/api/leads/{id}/status", leadId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"status":%d}
                                """.formatted(status)))
                .andExpect(status().isOk());
    }

    private void assertAuditRecordExists(String actionName) {
        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE module_name = 'LEAD' AND action_name = ?",
                Integer.class, actionName);
        Assertions.assertNotNull(auditCount);
        Assertions.assertTrue(auditCount > 0, "审计日志应存在 %s 记录".formatted(actionName));
    }
}
