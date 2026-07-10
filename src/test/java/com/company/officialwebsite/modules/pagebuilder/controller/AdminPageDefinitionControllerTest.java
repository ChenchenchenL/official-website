package com.company.officialwebsite.modules.pagebuilder.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionUpdateDTO;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

/**
 * AdminPageDefinitionControllerTest: 页面生命周期后台控制器集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPageDefinitionControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getPageList_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/page-builder/pages"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void getPageList_shouldReturnList_whenLoggedInAsAdmin() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/page-builder/pages")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void createPage_shouldSucceed_whenValidInput() throws Exception {
        MockHttpSession session = loginAsAdmin();

        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey("news-center");
        createDTO.setName("新闻中心");
        createDTO.setRoutePath("/news");
        createDTO.setPageType("NORMAL");
        createDTO.setVisible(true);
        createDTO.setSortOrder(10);

        mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.pageKey").value("news-center"))
                .andExpect(jsonPath("$.data.routePath").value("/news"));
    }

    @Test
    void createPage_shouldThrow_whenPageKeyIsBlank() throws Exception {
        MockHttpSession session = loginAsAdmin();

        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey(""); // Blank
        createDTO.setName("新闻中心");
        createDTO.setRoutePath("/news");
        createDTO.setPageType("NORMAL");
        createDTO.setVisible(true);
        createDTO.setSortOrder(10);

        mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.COMMON_PARAM_INVALID.getCode()));
    }

    @Test
    void createPage_shouldThrow_whenPageTypeIsInvalid() throws Exception {
        MockHttpSession session = loginAsAdmin();

        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey("news-center");
        createDTO.setName("新闻中心");
        createDTO.setRoutePath("/news");
        createDTO.setPageType("INVALID_TYPE"); // Invalid
        createDTO.setVisible(true);
        createDTO.setSortOrder(10);

        mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.COMMON_PARAM_INVALID.getCode()));
    }

    @Test
    void createThenUpdateAndDelete_shouldWorkAsExpected() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 1. 创建页面
        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey("about-us");
        createDTO.setName("关于我们");
        createDTO.setRoutePath("/about");
        createDTO.setPageType("NORMAL");
        createDTO.setVisible(true);
        createDTO.setSortOrder(20);

        String createResponse = mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 获取返回的 id 和 version
        Number pageIdNum = objectMapper.readTree(createResponse).path("data").path("id").numberValue();
        Number versionNum = objectMapper.readTree(createResponse).path("data").path("version").numberValue();
        long pageId = pageIdNum.longValue();
        int version = versionNum.intValue();

        // 2. 详情查询
        mockMvc.perform(get("/admin/api/page-builder/pages/" + pageId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(pageId))
                .andExpect(jsonPath("$.data.name").value("关于我们"));

        // 3. 更新元数据
        PageDefinitionUpdateDTO updateDTO = new PageDefinitionUpdateDTO();
        updateDTO.setName("关于我们新版");
        updateDTO.setRoutePath("/about-new");
        updateDTO.setVisible(false);
        updateDTO.setSortOrder(15);
        updateDTO.setVersion(version);

        String updateResponse = mockMvc.perform(put("/admin/api/page-builder/pages/" + pageId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 获取最新的 version
        int newVersion = version + 1; // 乐观锁应该自增了

        // 4. 逻辑删除
        mockMvc.perform(delete("/admin/api/page-builder/pages/" + pageId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(newVersion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        // 5. 再次查询应该不存在了 (404)
        mockMvc.perform(get("/admin/api/page-builder/pages/" + pageId)
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_NOT_FOUND.getCode()));
    }
}
