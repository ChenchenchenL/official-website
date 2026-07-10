package com.company.officialwebsite.modules.pagebuilder.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * AdminPageDraftControllerTest：验证后台页面草稿管理接口的安全拦截、草稿保存、预览Token生成与前台预览读取。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPageDraftControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void insertMockMedia() {
        jdbcTemplate.execute("DELETE FROM media_asset WHERE id IN (2002, 5001)");
        jdbcTemplate.execute("INSERT INTO media_asset (id, media_type, status, original_filename, content_type, storage_path, public_url, file_size, version, deleted_marker, created_at, updated_at) " +
                "VALUES (2002, 'IMAGE', 'BOUND', 'test.jpg', 'image/jpeg', '/test.jpg', 'http://localhost/test.jpg', 1024, 1, 0, NOW(), NOW())");
        jdbcTemplate.execute("INSERT INTO media_asset (id, media_type, status, original_filename, content_type, storage_path, public_url, file_size, version, deleted_marker, created_at, updated_at) " +
                "VALUES (5001, 'IMAGE', 'BOUND', 'test2.jpg', 'image/jpeg', '/test2.jpg', 'http://localhost/test2.jpg', 2048, 1, 0, NOW(), NOW())");
        
        jdbcTemplate.execute("DELETE FROM cms_page_dependency WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'contact-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_publish_snapshot WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'contact-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_version WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'contact-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_draft WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'contact-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_definition WHERE page_key = 'contact-page'");
    }

    @Test
    void getDraft_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/page-builder/drafts/999"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void getDraft_shouldReturn404_whenPageNotFound() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/admin/api/page-builder/drafts/999999999")
                        .session(session))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_DRAFT_NOT_FOUND.getCode()));
    }

    @Test
    void fullDraftAndPreviewWorkflow_shouldWorkAsExpected() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 1. 创建一个页面，这会同步初始化空白草稿
        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey("contact-page");
        createDTO.setName("联系我们页面");
        createDTO.setRoutePath("/contact");
        createDTO.setPageType("NORMAL");
        createDTO.setVisible(true);
        createDTO.setSortOrder(30);

        String createResponse = mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Number pageIdNum = objectMapper.readTree(createResponse).path("data").path("id").numberValue();
        long pageId = pageIdNum.longValue();

        // 2. 查询草稿，应该有初始化空白草稿，version = 0
        String getDraftResponse = mockMvc.perform(get("/admin/api/page-builder/drafts/" + pageId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageId").value(pageId))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.data.schemaJson").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        int draftVersion = objectMapper.readTree(getDraftResponse).path("data").path("version").asInt();

        // 3. 准备更新草稿的 Schema 数据
        PageSchemaModel schema = new PageSchemaModel();
        schema.setPageKey("contact-page");
        schema.setName("联系我们页面");
        
        LayoutModel layout = new LayoutModel();
        layout.setType("vertical");
        schema.setLayout(layout);

        SectionModel section = new SectionModel();
        section.setId("hero_section");
        section.setComponent("HeroBanner");
        section.setVisible(true);
        Map<String, Object> props = new HashMap<>();
        props.put("title", "联系我们");
        props.put("backgroundMediaId", "2002");
        props.put("primaryButtonLink", "/");
        section.setProps(props);
        schema.setSections(Collections.singletonList(section));

        PageDraftSaveDTO saveDTO = new PageDraftSaveDTO();
        saveDTO.setSchemaJson(schema);
        saveDTO.setEditorSessionRemark("保存首版草稿配置");
        saveDTO.setVersion(draftVersion);

        // 4. 保存草稿
        String saveResponse = mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.schemaJson.name").value("联系我们页面"))
                .andExpect(jsonPath("$.data.schemaJson.sections[0].id").value("hero_section"))
                .andReturn().getResponse().getContentAsString();

        // 5. 生成预览 Token
        String tokenResponse = mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/preview-token")
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isString())
                .andReturn().getResponse().getContentAsString();

        String previewToken = objectMapper.readTree(tokenResponse).path("data").asText();

        // 6. 前台提取预览数据（无需登录）
        mockMvc.perform(get("/portal/api/page-builder/pages/preview")
                        .param("previewToken", previewToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageKey").value("contact-page"))
                .andExpect(jsonPath("$.data.name").value("联系我们页面"))
                .andExpect(jsonPath("$.data.schemaJson.sections[0].props.title").value("联系我们"));

        // 7. 测试错误的/过期的 Token（应该返回 404）
        mockMvc.perform(get("/portal/api/page-builder/pages/preview")
                        .param("previewToken", "invalid-token-12345"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED.getCode()));
    }
}
