package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPagePublishSecurityTest {

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

    // -----------------------------------------------------------------------
    // 辅助方法
    // -----------------------------------------------------------------------

    private PageDefinitionEntity createPage(String key) {
        PageDefinitionEntity page = new PageDefinitionEntity();
        page.setPageKey(key);
        page.setName("发布测试_" + key);
        page.setRoutePath("/" + key);
        page.setPageType("NORMAL");
        page.setStatus("ENABLED");
        page.setVisible(true);
        page.setSortOrder(1);
        pageDefinitionMapper.insert(page);
        return page;
    }

    private PageDraftEntity createDraft(Long pageId, String hash) {
        PageSchemaModel schema = new PageSchemaModel();
        schema.setPageKey("key_" + pageId);
        schema.setName("schema_" + pageId);
        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(pageId);
        draft.setSchemaJson(schema);
        draft.setSchemaHash(hash);
        draft.setVersion(0);
        pageDraftMapper.insert(draft);
        return draft;
    }

    // -----------------------------------------------------------------------
    // 1. 缺少锁 Header -> 400/10011
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("缺少锁 Header 时发布页面返回 400/10011")
    void publish_missingLockHeader_shouldReturn400() throws Exception {
        mockMvc.perform(post("/admin/api/page-builder/pages/1/publish")
                        .with(csrf())
                        .with(user("admin").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"测试发布\",\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.EDITOR_LOCK_TOKEN_REQUIRED.getCode()));
    }

    // -----------------------------------------------------------------------
    // 2. 错误锁 Token -> 403/10009
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("错误锁 Token 发布页面返回 403/10009")
    void publish_wrongLockToken_shouldReturn403And10009() throws Exception {
        PageDefinitionEntity page = createPage("pub_wrong_token");
        createDraft(page.getId(), "hash_wt");
        editorLockService.acquireLock(EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_wt", "管理员", false);

        mockMvc.perform(post("/admin/api/page-builder/pages/" + page.getId() + "/publish")
                        .with(csrf())
                        .with(user("admin_wt").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", "totally_wrong_token_abc123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"错误锁发布\",\"version\":0}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.EDITOR_LOCK_OWNER_MISMATCH.getCode()));
    }

    // -----------------------------------------------------------------------
    // 3. 版本冲突发布 -> 409/10003
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("版本冲突发布返回 409/10003")
    void publish_versionConflict_shouldReturn409() throws Exception {
        PageDefinitionEntity page = createPage("pub_ver_conflict");
        PageDraftEntity draft = createDraft(page.getId(), "hash_vc");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_vc", "管理员", false);

        // 当前草稿版本为 0，传入错误版本 99
        mockMvc.perform(post("/admin/api/page-builder/pages/" + page.getId() + "/publish")
                        .with(csrf())
                        .with(user("admin_vc").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"版本冲突发布\",\"version\":99}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.COMMON_STATE_CONFLICT.getCode()));
    }

    // -----------------------------------------------------------------------
    // 4. 发布缺少 version 字段 -> 400
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("发布缺少 version 字段返回 400")
    void publish_missingVersion_shouldReturn400() throws Exception {
        PageDefinitionEntity page = createPage("pub_no_version");
        createDraft(page.getId(), "hash_nv");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_nv", "管理员", false);

        mockMvc.perform(post("/admin/api/page-builder/pages/" + page.getId() + "/publish")
                        .with(csrf())
                        .with(user("admin_nv").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"缺版本发布\"}"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // 5. 持锁管理员可以成功发布页面
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("持锁管理员携带正确 version 成功发布")
    void publish_withLockAndCorrectVersion_shouldSuccess() throws Exception {
        PageDefinitionEntity page = createPage("pub_success");
        PageDraftEntity draft = createDraft(page.getId(), "hash_pub_123");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_pub", "发布员", false);

        mockMvc.perform(post("/admin/api/page-builder/pages/" + page.getId() + "/publish")
                        .with(csrf())
                        .with(user("admin_pub").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"测试持锁发布\",\"version\":" + draft.getVersion() + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").exists())
                .andExpect(jsonPath("$.data.rollbackSourceVersionId").doesNotExist());
    }

    // -----------------------------------------------------------------------
    // 6. 发布后回滚，rollbackSourceVersionId 有值
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("回滚后新版本携带 rollbackSourceVersionId")
    void rollback_shouldSetRollbackSourceVersionId() throws Exception {
        PageDefinitionEntity page = createPage("rollback_test");
        PageDraftEntity draft = createDraft(page.getId(), "hash_rb_001");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_rb", "回滚员", false);

        // 先发布一次，得到 versionId
        String pubResp = mockMvc.perform(post("/admin/api/page-builder/pages/" + page.getId() + "/publish")
                        .with(csrf())
                        .with(user("admin_rb").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"changeSummary\":\"初次发布\",\"version\":" + draft.getVersion() + "}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long versionId = objectMapper.readTree(pubResp).path("data").path("id").asLong();

        // 重新加锁（发布后草稿版本已更新，需重新获取锁）
        LockStatusVO lock2 = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_rb", "回滚员", false);

        // 读最新草稿版本
        PageDraftEntity freshDraft = pageDraftMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PageDraftEntity>()
                        .eq(PageDraftEntity::getPageId, page.getId()));

        // 回滚到刚才发布的版本
        mockMvc.perform(post("/admin/api/page-builder/pages/" + page.getId() + "/rollback")
                        .with(csrf())
                        .with(user("admin_rb").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock2.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"versionId\":" + versionId + ",\"version\":" + freshDraft.getVersion() + ",\"changeSummary\":\"回滚测试\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.rollbackSourceVersionId").value(versionId));
    }

    // -----------------------------------------------------------------------
    // 7. 回滚缺少 versionId 字段 -> 400
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("回滚缺少 versionId 返回 400")
    void rollback_missingVersionId_shouldReturn400() throws Exception {
        PageDefinitionEntity page = createPage("rollback_no_vid");
        createDraft(page.getId(), "hash_rnv");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, page.getId(), null, "admin_rnv", "管理员", false);

        mockMvc.perform(post("/admin/api/page-builder/pages/" + page.getId() + "/rollback")
                        .with(csrf())
                        .with(user("admin_rnv").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"changeSummary\":\"缺versionId\"}"))
                .andExpect(status().isBadRequest());
    }
}
