package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPageDraftSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EditorLockService editorLockService;

    @Autowired
    private PageDefinitionMapper pageDefinitionMapper;

    @Autowired
    private PageDraftMapper pageDraftMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("缺少 X-Editor-Lock-Token Header 时保存草稿返回 400 Bad Request")
    void saveDraft_missingLockHeaderShouldReturn400() throws Exception {
        mockMvc.perform(put("/admin/api/page-builder/drafts/1")
                        .with(csrf())
                        .with(user("admin").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"schemaJson\":{\"pageKey\":\"home\",\"name\":\"首页\",\"layout\":{\"type\":\"default\"},\"sections\":[{\"id\":\"sec1\",\"component\":\"HeroBanner\",\"props\":{\"title\":\"欢迎\"}}]}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10011));
    }

    @Test
    @DisplayName("持锁管理员可以成功保存草稿")
    void saveDraft_withValidLockShouldSuccess() throws Exception {
        PageDefinitionEntity page = new PageDefinitionEntity();
        page.setPageKey("test_page_key_1");
        page.setName("测试页面");
        page.setRoutePath("/test-page-1");
        page.setPageType("NORMAL");
        page.setStatus("ENABLED");
        page.setVisible(true);
        page.setSortOrder(1);
        pageDefinitionMapper.insert(page);

        PageSchemaModel schema = new PageSchemaModel();
        schema.setPageKey("test_page_key_1");
        schema.setName("测试页面");

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(page.getId());
        draft.setSchemaJson(schema);
        draft.setSchemaHash("dummy_hash");
        pageDraftMapper.insert(draft);

        LockStatusVO lock = editorLockService.acquireLock(EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_editor", "编辑员", false);

        mockMvc.perform(put("/admin/api/page-builder/drafts/" + page.getId())
                        .with(csrf())
                        .with(user("admin_editor").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"schemaJson\":{\"pageKey\":\"test_page_key_1\",\"name\":\"测试页面\",\"layout\":{\"type\":\"default\"},\"sections\":[{\"id\":\"sec1\",\"component\":\"HeroBanner\",\"props\":{\"title\":\"欢迎\"}}]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("保存草稿版本冲突时返回 409 且携带最新草稿数据 payload")
    void saveDraft_versionConflict_shouldReturn409WithDraftData() throws Exception {
        PageDefinitionEntity page = new PageDefinitionEntity();
        page.setPageKey("conflict_page_1");
        page.setName("冲突测试页面");
        page.setRoutePath("/conflict-page-1");
        page.setPageType("NORMAL");
        page.setStatus("ENABLED");
        page.setVisible(true);
        page.setSortOrder(1);
        pageDefinitionMapper.insert(page);

        PageSchemaModel schema = new PageSchemaModel();
        schema.setPageKey("conflict_page_1");
        schema.setName("冲突测试页面");

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(page.getId());
        draft.setSchemaJson(schema);
        draft.setSchemaHash("hash_ver_0");
        draft.setVersion(5); // DB 中当前草稿版本为 5
        pageDraftMapper.insert(draft);

        LockStatusVO lock = editorLockService.acquireLock(EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_cf", "编辑员", false);

        // 请求中传入过期版本 2
        mockMvc.perform(put("/admin/api/page-builder/drafts/" + page.getId())
                        .with(csrf())
                        .with(user("admin_cf").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":2,\"schemaJson\":{\"pageKey\":\"conflict_page_1\",\"name\":\"冲突测试页面\",\"layout\":{\"type\":\"default\"},\"sections\":[]}}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10003))
                .andExpect(jsonPath("$.data.version").value(5))
                .andExpect(jsonPath("$.data.schemaHash").value("hash_ver_0"));
    }

    @Test
    @DisplayName("缺少 X-Editor-Lock-Token Header 时重置草稿返回 400 Bad Request")
    void resetDraftToPublished_missingLockHeader_shouldReturn400() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/api/page-builder/drafts/1/reset-to-published")
                        .with(csrf())
                        .with(user("admin").roles("ADMINISTRATOR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10011));
    }

    @Test
    @DisplayName("错误锁 Token 重置草稿返回 403 Forbidden")
    void resetDraftToPublished_wrongLockToken_shouldReturn403() throws Exception {
        PageDefinitionEntity page = new PageDefinitionEntity();
        page.setPageKey("reset_lock_test");
        page.setName("锁重置测试");
        page.setRoutePath("/reset-lock-test");
        page.setPageType("NORMAL");
        page.setStatus("ENABLED");
        page.setVisible(true);
        page.setSortOrder(1);
        pageDefinitionMapper.insert(page);

        editorLockService.acquireLock(EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_owner", "管理员", false);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/admin/api/page-builder/drafts/" + page.getId() + "/reset-to-published")
                        .with(csrf())
                        .with(user("admin_owner").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", "invalid_lock_token_999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(10009));
    }
}
