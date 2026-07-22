package com.company.officialwebsite.modules.pagebuilder.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
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
 * AdminPageDraftControllerTest：验证后台页面草稿管理接口的安全拦截、草稿保存能力。
 * 预览 Token 相关测试已迁移至 {@link PortalPreviewControllerTest}。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPageDraftControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EditorLockService editorLockService;

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
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_NOT_FOUND.getCode()));
    }

    @Test
    void saveDraft_shouldSucceed_andReturnUpdatedVersion() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 1. 创建页面，自动初始化空白草稿
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

        long pageId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        // 2. 查询草稿，version 应为 0
        String getDraftResponse = mockMvc.perform(get("/admin/api/page-builder/drafts/" + pageId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageId").value(pageId))
                .andExpect(jsonPath("$.data.version").value(0))
                .andReturn().getResponse().getContentAsString();

        int draftVersion = objectMapper.readTree(getDraftResponse).path("data").path("version").asInt();

        // 2.5 获取页面独占编辑锁（S2/S3 安全约束：保存草稿必须持锁并携带 X-Editor-Lock-Token）
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);

        // 3. 保存草稿
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

        mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.data.schemaJson.name").value("联系我们页面"))
                .andExpect(jsonPath("$.data.schemaJson.sections[0].id").value("hero_section"));
    }

    @Test
    void resetDraftToPublished_shouldRevertDraftToActiveSnapshot() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 1. 创建页面
        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey("contact-page");
        createDTO.setName("联系我们");
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

        long pageId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);

        // 2. 在未发布状态下重置，应返回 404
        mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/reset-to-published")
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.COMMON_RESOURCE_NOT_FOUND.getCode()));

        // 3. 先保存一份可发布的首版草稿
        PageSchemaModel publishedSchema = new PageSchemaModel();
        publishedSchema.setPageKey("contact-page");
        publishedSchema.setName("联系我们");
        LayoutModel publishedLayout = new LayoutModel();
        publishedLayout.setType("vertical");
        publishedSchema.setLayout(publishedLayout);
        SectionModel publishedSection = new SectionModel();
        publishedSection.setId("contact_hero");
        publishedSection.setComponent("HeroBanner");
        publishedSection.setVisible(true);
        publishedSection.setProps(Map.of("title", "联系我们"));
        publishedSchema.setSections(Collections.singletonList(publishedSection));

        PageDraftSaveDTO publishedDraftDTO = new PageDraftSaveDTO();
        publishedDraftDTO.setSchemaJson(publishedSchema);
        publishedDraftDTO.setEditorSessionRemark("待发布首版草稿");
        publishedDraftDTO.setVersion(0);

        mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishedDraftDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(1));

        // 4. 发布首版草稿
        com.company.officialwebsite.modules.pagebuilder.dto.PagePublishDTO pubDTO = new com.company.officialwebsite.modules.pagebuilder.dto.PagePublishDTO();
        pubDTO.setChangeSummary("发布首版");
        pubDTO.setVersion(1);

        mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/publish")
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pubDTO)))
                .andExpect(status().isOk());

        // 5. 修改草稿
        PageSchemaModel modifiedSchema = new PageSchemaModel();
        modifiedSchema.setPageKey("contact-page");
        modifiedSchema.setName("修改后的未知草稿");
        LayoutModel modifiedLayout = new LayoutModel();
        modifiedLayout.setType("vertical");
        modifiedSchema.setLayout(modifiedLayout);
        SectionModel modifiedSection = new SectionModel();
        modifiedSection.setId("contact_hero_changed");
        modifiedSection.setComponent("HeroBanner");
        modifiedSection.setVisible(true);
        modifiedSection.setProps(Map.of("title", "修改后的未知草稿"));
        modifiedSchema.setSections(Collections.singletonList(modifiedSection));

        PageDraftSaveDTO saveDTO = new PageDraftSaveDTO();
        saveDTO.setSchemaJson(modifiedSchema);
        saveDTO.setEditorSessionRemark("未发布的临时草稿");
        saveDTO.setVersion(1);

        mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schemaJson.name").value("修改后的未知草稿"));

        // 6. 重置草稿为已发布快照
        mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/reset-to-published")
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.schemaJson.name").value("联系我们"))
                .andExpect(jsonPath("$.data.editorSessionRemark").value("重置草稿为当前已发布版本"));
    }
}
