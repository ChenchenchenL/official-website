package com.company.officialwebsite.modules.pagebuilder.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PagePublishDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageRollbackDTO;
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
 * AdminPagePublishControllerTest: 页面发布、版本与回滚集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPagePublishControllerTest extends BaseAdminControllerIntegrationTest {

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

        jdbcTemplate.execute("DELETE FROM cms_page_dependency WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'solutions-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_publish_snapshot WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'solutions-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_version WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'solutions-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_draft WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'solutions-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_definition WHERE page_key = 'solutions-page'");
    }

    @Test
    void getVersions_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/page-builder/pages/1/versions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void fullPublishAndRollbackWorkflow_shouldSucceed() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 1. 创建页面元数据
        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey("solutions-page");
        createDTO.setName("解决方案页面");
        createDTO.setRoutePath("/solutions");
        createDTO.setPageType("NORMAL");
        createDTO.setVisible(true);
        createDTO.setSortOrder(50);

        String createResponse = mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long pageId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        // 2. 获取编辑锁（S2/S3：草稿保存、发布、回滚均需持锁并携带 X-Editor-Lock-Token）
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);

        // 3. 保存合法的草稿 Schema
        PageSchemaModel schema = new PageSchemaModel();
        schema.setPageKey("solutions-page");
        schema.setName("解决方案页面");

        LayoutModel layout = new LayoutModel();
        layout.setType("vertical");
        schema.setLayout(layout);

        SectionModel section = new SectionModel();
        section.setId("solution_sec_1");
        section.setComponent("HeroBanner");
        section.setVisible(true);
        Map<String, Object> props = new HashMap<>();
        props.put("title", "我们的解决方案");
        props.put("backgroundMediaId", "5001");
        props.put("primaryButtonLink", "/");
        section.setProps(props);
        schema.setSections(Collections.singletonList(section));

        PageDraftSaveDTO saveDTO = new PageDraftSaveDTO();
        saveDTO.setSchemaJson(schema);
        saveDTO.setEditorSessionRemark("解决方案草稿一");
        saveDTO.setVersion(0);

        mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk());

        // 4. 发布草稿（携带锁 Token，version=1 为 saveDraft 后的草稿版本号）
        PagePublishDTO publishDTO = new PagePublishDTO();
        publishDTO.setChangeSummary("发布首个正式版本的解决方案");
        publishDTO.setVersion(1);

        String publishResponse = mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/publish")
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andExpect(jsonPath("$.data.sourceType").value("PUBLISH_BASE"))
                .andExpect(jsonPath("$.data.changeSummary").value("发布首个正式版本的解决方案"))
                .andReturn().getResponse().getContentAsString();

        long versionId = objectMapper.readTree(publishResponse).path("data").path("id").asLong();

        // 5. 查询版本列表（分页摘要），应包含刚刚发布的 version 1，且 schemaJson 为空
        mockMvc.perform(get("/admin/api/page-builder/pages/" + pageId + "/versions?pageNo=1&pageSize=10")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.list").isArray())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].id").value(versionId))
                .andExpect(jsonPath("$.data.list[0].versionNo").value(1))
                .andExpect(jsonPath("$.data.list[0].schemaJson").doesNotExist());

        // 5.5 单版本详情查询（包含完整 schemaJson）
        mockMvc.perform(get("/admin/api/page-builder/pages/" + pageId + "/versions/" + versionId)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(versionId))
                .andExpect(jsonPath("$.data.schemaJson.name").value("解决方案页面"));

        // 6. 保存并发布第二版，使版本 1 成为可回滚的历史版本。
        schema.setName("解决方案页面第二版");
        props.put("title", "我们的解决方案第二版");
        PageDraftSaveDTO secondDraftDTO = new PageDraftSaveDTO();
        secondDraftDTO.setSchemaJson(schema);
        secondDraftDTO.setEditorSessionRemark("解决方案草稿二");
        secondDraftDTO.setVersion(1);
        mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondDraftDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.version").value(2));

        PagePublishDTO secondPublishDTO = new PagePublishDTO();
        secondPublishDTO.setChangeSummary("发布第二版解决方案");
        secondPublishDTO.setVersion(2);
        mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/publish")
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(secondPublishDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNo").value(2));

        // 7. 执行回滚操作到版本 1（JSON 请求体；version 为第二版草稿版本号）
        PageRollbackDTO rollbackDTO = new PageRollbackDTO();
        rollbackDTO.setVersionId(versionId);
        rollbackDTO.setVersion(2);
        rollbackDTO.setChangeSummary("回滚至版本 No.1 (变更说明: 发布首个正式版本的解决方案)");

        mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/rollback")
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rollbackDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.versionNo").value(3))
                .andExpect(jsonPath("$.data.sourceType").value("ROLLBACK_BASE"))
                .andExpect(jsonPath("$.data.changeSummary").value("回滚至版本 No.1 (变更说明: 发布首个正式版本的解决方案)"));

        // 8. 版本列表里现在有 3 个版本（回滚版本 3 在首位）
        mockMvc.perform(get("/admin/api/page-builder/pages/" + pageId + "/versions")
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(3))
                .andExpect(jsonPath("$.data.list[0].versionNo").value(3))
                .andExpect(jsonPath("$.data.list[1].versionNo").value(2))
                .andExpect(jsonPath("$.data.list[2].versionNo").value(1));
    }

    @Test
    void publishAndRollback_idempotencyTest() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 1. 创建页面定义
        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey("idempotency-page");
        createDTO.setName("幂等性测试页面");
        createDTO.setRoutePath("/idempotency");
        createDTO.setPageType("NORMAL");
        createDTO.setVisible(true);
        createDTO.setSortOrder(100);

        String createResp = mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long pageId = objectMapper.readTree(createResp).path("data").path("id").asLong();

        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);

        // 2. 保存草稿
        PageSchemaModel schema = new PageSchemaModel();
        schema.setPageKey("idempotency-page");
        schema.setName("幂等性测试页面");
        LayoutModel layout = new LayoutModel();
        layout.setType("vertical");
        schema.setLayout(layout);
        SectionModel section = new SectionModel();
        section.setId("idempotency_hero");
        section.setComponent("HeroBanner");
        section.setVisible(true);
        section.setProps(Map.of("title", "幂等性测试"));
        schema.setSections(Collections.singletonList(section));

        PageDraftSaveDTO saveDTO = new PageDraftSaveDTO();
        saveDTO.setSchemaJson(schema);
        saveDTO.setEditorSessionRemark("幂等草稿");
        saveDTO.setVersion(0);

        mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk());

        // 3. 首次发布
        PagePublishDTO publishDTO = new PagePublishDTO();
        publishDTO.setChangeSummary("首次发布");
        publishDTO.setVersion(1);

        String firstPubResp = mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/publish")
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andReturn().getResponse().getContentAsString();

        long firstVersionId = objectMapper.readTree(firstPubResp).path("data").path("id").asLong();

        // 4. 重复发布（同一草稿 Schema），应当幂等成功并返回相同版本，不产生新版本号
        mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/publish")
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstVersionId))
                .andExpect(jsonPath("$.data.versionNo").value(1));

        // 5. 重复回滚到当前已在线的版本，应当幂等成功并返回该版本
        PageRollbackDTO rollbackDTO = new PageRollbackDTO();
        rollbackDTO.setVersionId(firstVersionId);
        rollbackDTO.setVersion(1);
        rollbackDTO.setChangeSummary("重复回滚已在线版本");

        mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/rollback")
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(rollbackDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(firstVersionId))
                .andExpect(jsonPath("$.data.versionNo").value(1));
    }
}
