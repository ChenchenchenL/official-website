package com.company.officialwebsite.modules.lead.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * ContactInfoControllerTest：验证基础联系方式后台维护与前台读取的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ContactInfoControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update(
                "UPDATE cms_contact_info SET contact_address = '武汉市东湖新技术开发区光谷大道77号', "
                        + "business_phone = '+86 027-88886666', contact_email = 'business@example.com', "
                        + "version = 0, deleted_marker = 0 WHERE config_key = 'default' AND id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build("contact_info"));
    }

    @Test
    void adminGetContactInfo_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/contact-info"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void adminGetContactInfo_shouldReturnDefaultContactInfo() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/contact-info").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.contactAddress").isNotEmpty())
                .andExpect(jsonPath("$.data.businessPhone").isNotEmpty())
                .andExpect(jsonPath("$.data.contactEmail").isNotEmpty())
                .andExpect(jsonPath("$.data.version").value(0));
    }

    @Test
    void portalGetContactInfo_shouldReturnPublicFields() throws Exception {
        mockMvc.perform(get("/portal/api/contact-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.contactAddress").value("武汉市东湖新技术开发区光谷大道77号"))
                .andExpect(jsonPath("$.data.businessPhone").value("+86 027-88886666"))
                .andExpect(jsonPath("$.data.contactEmail").value("business@example.com"))
                .andExpect(jsonPath("$.data.id").doesNotExist())
                .andExpect(jsonPath("$.data.version").doesNotExist())
                .andExpect(jsonPath("$.data.createdBy").doesNotExist())
                .andExpect(jsonPath("$.data.updatedBy").doesNotExist())
                .andExpect(jsonPath("$.data.deletedMarker").doesNotExist());
    }

    @Test
    void updateContactInfo_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"contactAddress":"北京市朝阳区建国路1号",
                                 "businessPhone":"+86 010-66665555","contactEmail":"bj@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/contact-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactAddress").value("北京市朝阳区建国路1号"))
                .andExpect(jsonPath("$.data.businessPhone").value("+86 010-66665555"))
                .andExpect(jsonPath("$.data.contactEmail").value("bj@example.com"));
    }

    @Test
    void updateContactInfo_shouldRejectVersionConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":999,"contactAddress":"北京市朝阳区建国路1号",
                                 "businessPhone":"+86 010-66665555","contactEmail":"bj@example.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void updateContactInfo_shouldRejectBlankAddress() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"contactAddress":"   ",
                                 "businessPhone":"+86 027-88886666","contactEmail":"business@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void updateContactInfo_shouldRejectPhoneWithIllegalChars() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"contactAddress":"武汉市东湖新技术开发区光谷大道77号",
                                 "businessPhone":"abc-defg","contactEmail":"business@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void updateContactInfo_shouldRejectPhoneWithoutAnyDigit() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"contactAddress":"武汉市东湖新技术开发区光谷大道77号",
                                 "businessPhone":"+--()/#","contactEmail":"business@example.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void updateContactInfo_shouldRejectInvalidEmail() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"contactAddress":"武汉市东湖新技术开发区光谷大道77号",
                                 "businessPhone":"+86 027-88886666","contactEmail":"not-an-email"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void portalGetContactInfo_shouldReflectUpdateAfterCacheInvalidation() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(get("/portal/api/contact-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactAddress").value("武汉市东湖新技术开发区光谷大道77号"));

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"contactAddress":"深圳市南山区科技园路1号",
                                 "businessPhone":"+86 0755-88886666","contactEmail":"sz@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/contact-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.contactAddress").value("深圳市南山区科技园路1号"))
                .andExpect(jsonPath("$.data.businessPhone").value("+86 0755-88886666"))
                .andExpect(jsonPath("$.data.contactEmail").value("sz@example.com"));
    }

    @Test
    void adminGetContactInfo_shouldReturn70001_whenDefaultRecordMissing() throws Exception {
        jdbcTemplate.update("UPDATE cms_contact_info SET deleted_marker = id WHERE config_key = 'default' AND id > 0");

        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/contact-info").session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(TestConstants.LEAD_CONTACT_INFO_NOT_FOUND));
    }

    @Test
    void portalGetContactInfo_shouldReturn70001_whenDefaultRecordMissing() throws Exception {
        jdbcTemplate.update("UPDATE cms_contact_info SET deleted_marker = id WHERE config_key = 'default' AND id > 0");

        mockMvc.perform(get("/portal/api/contact-info"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(TestConstants.LEAD_CONTACT_INFO_NOT_FOUND));
    }

    @Test
    void updateContactInfo_shouldWriteAuditLog() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"contactAddress":"武汉市东湖新技术开发区光谷大道77号",
                                 "businessPhone":"+86 027-88886666","contactEmail":"business@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE module_name = 'LEAD' "
                        + "AND action_name = 'UPDATE_CONTACT_INFO' AND target_type = 'CONTACT_INFO'",
                Integer.class);
        Assertions.assertNotNull(auditCount);
        Assertions.assertTrue(auditCount > 0, "审计日志应存在 UPDATE_CONTACT_INFO 记录");
    }

    @Test
    void updateContactInfo_shouldAcceptAllowedPhoneChars() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"contactAddress":"武汉市东湖新技术开发区光谷大道77号",
                                 "businessPhone":"+86 (027) 8888-6666/123","contactEmail":"business@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
    }

    @Test
    void updateContactInfo_shouldIncrementVersionAfterSuccess() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/contact-info")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"contactAddress":"武汉市东湖新技术开发区光谷大道77号",
                                 "businessPhone":"+86 027-88886666","contactEmail":"business@example.com"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_contact_info WHERE config_key = 'default' AND deleted_marker = 0",
                Integer.class);
        Assertions.assertNotNull(version);
        Assertions.assertEquals(1, version, "更新后版本号应递增");
    }
}
