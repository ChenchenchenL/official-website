package com.company.officialwebsite.modules.pagebuilder.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminComponentTemplateControllerTest：验证后台组件物料管理接口的安全拦截与数据返回。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminComponentTemplateControllerTest extends BaseAdminControllerIntegrationTest {

    @Test
    void getTemplates_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/page-builder/component-templates"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void getTemplates_shouldReturnList_whenLoggedInAsAdmin() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/page-builder/component-templates")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(14))
                .andExpect(jsonPath("$.data[0].componentCode").value("HeroBanner"));
    }

    @Test
    void getTemplateDetail_shouldReturnDetails_whenExists() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/page-builder/component-templates/HeroBanner")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.componentCode").value("HeroBanner"))
                .andExpect(jsonPath("$.data.name").value("首屏主视觉"));
    }

    @Test
    void getTemplateDetail_shouldReturnNotFound_whenNotExists() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/page-builder/component-templates/InvalidCode")
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.COMMON_RESOURCE_NOT_FOUND.getCode()));
    }

    @Test
    void getTemplateUsage_shouldRequireAdminAndReturnPagedUsage() throws Exception {
        mockMvc.perform(get("/admin/api/page-builder/component-templates/HeroBanner/usage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));

        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/page-builder/component-templates/HeroBanner/usage")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.componentCode").value("HeroBanner"))
                .andExpect(jsonPath("$.data.activeSnapshotPages.total").value(0))
                .andExpect(jsonPath("$.data.draftPages.total").value(0));
    }
}
