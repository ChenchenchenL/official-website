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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPreviewSecurityTest {

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
    // 辅助：构建页面 + 草稿 + 编辑锁
    // -----------------------------------------------------------------------

    private PageSetup setupPage(String key, String hash) throws Exception {
        PageDefinitionEntity page = new PageDefinitionEntity();
        page.setPageKey(key);
        page.setName("测试页面_" + key);
        page.setRoutePath("/" + key);
        page.setPageType("NORMAL");
        page.setStatus("ENABLED");
        page.setVisible(true);
        page.setSortOrder(1);
        pageDefinitionMapper.insert(page);

        PageSchemaModel schema = new PageSchemaModel();
        schema.setPageKey(key);
        schema.setName("测试页面_" + key);

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(page.getId());
        draft.setSchemaJson(schema);
        draft.setSchemaHash(hash);
        pageDraftMapper.insert(draft);

        return new PageSetup(page, draft);
    }

    private record PageSetup(PageDefinitionEntity page, PageDraftEntity draft) {}

    // -----------------------------------------------------------------------
    // 1. 缺少 schemaHash 创建预览 -> 400
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("创建预览缺少 schemaHash 返回 400")
    void createPreview_missingSchemaHash_shouldReturn400() throws Exception {
        PageSetup setup = setupPage("preview_no_hash", "hash_001");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, setup.page().getId(), null, "admin_p1", "管理员", false);

        mockMvc.perform(post("/admin/api/page-builder/drafts/" + setup.page().getId() + "/previews")
                        .with(csrf())
                        .with(user("admin_p1").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // 2. schemaHash 不匹配 -> 400/90006
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("创建预览 schemaHash 不匹配返回 400/90006")
    void createPreview_hashMismatch_shouldReturn400And90006() throws Exception {
        PageSetup setup = setupPage("preview_hash_mismatch", "server_hash_abc");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, setup.page().getId(), null, "admin_p2", "管理员", false);

        mockMvc.perform(post("/admin/api/page-builder/drafts/" + setup.page().getId() + "/previews")
                        .with(csrf())
                        .with(user("admin_p2").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaHash\":\"wrong_hash_xyz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_PREVIEW_SCHEMA_HASH_MISMATCH.getCode()));
    }

    // -----------------------------------------------------------------------
    // 3. 完整预览流程：生成、Portal 访问、无缓存头、撤销
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("受控预览完整流程：生成 -> 未登录拒绝 -> 创建者访问成功 -> 撤销")
    void previewSecurity_fullFlowTest() throws Exception {
        PageSetup setup = setupPage("preview_full_flow", "hash_prev_123");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, setup.page().getId(), null, "admin_preview", "预览管理员", false);

        // 生成预览 Token（schemaHash 必须与草稿一致）
        String content = mockMvc.perform(post("/admin/api/page-builder/drafts/" + setup.page().getId() + "/previews")
                        .with(csrf())
                        .with(user("admin_preview").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaHash\":\"hash_prev_123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewToken").exists())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(content).path("data").path("previewToken").asText();

        // 未登录访问 Portal 预览 -> 401
        mockMvc.perform(get("/portal/api/page-builder/previews/" + token))
                .andExpect(status().isUnauthorized());

        // 创建者登录访问 -> 200 + no-store 头
        mockMvc.perform(get("/portal/api/page-builder/previews/" + token)
                        .with(user("admin_preview").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        // 持锁撤销 -> 200
        mockMvc.perform(delete("/admin/api/page-builder/previews/" + token)
                        .with(csrf())
                        .with(user("admin_preview").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken()))
                .andExpect(status().isOk());
    }

    // -----------------------------------------------------------------------
    // 4. 非创建管理员访问预览 -> 403
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("非创建管理员访问预览返回 403")
    void previewAccess_byOtherAdmin_shouldReturn403() throws Exception {
        PageSetup setup = setupPage("preview_other_admin", "hash_other_001");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, setup.page().getId(), null, "creator_admin", "创建者", false);

        String content = mockMvc.perform(post("/admin/api/page-builder/drafts/" + setup.page().getId() + "/previews")
                        .with(csrf())
                        .with(user("creator_admin").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaHash\":\"hash_other_001\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(content).path("data").path("previewToken").asText();

        // 另一个管理员访问 -> 403
        mockMvc.perform(get("/portal/api/page-builder/previews/" + token)
                        .with(user("other_admin").roles("ADMINISTRATOR")))
                .andExpect(status().isForbidden());
    }

    // -----------------------------------------------------------------------
    // 5. 草稿更新后旧 Token 访问 Portal 预览 -> 401
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("草稿更新后旧预览 Token 访问返回 401")
    void previewAccess_afterDraftUpdated_shouldReturn401() throws Exception {
        PageSetup setup = setupPage("preview_stale", "hash_original");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, setup.page().getId(), null, "admin_stale", "管理员", false);

        // 生成预览
        String content = mockMvc.perform(post("/admin/api/page-builder/drafts/" + setup.page().getId() + "/previews")
                        .with(csrf())
                        .with(user("admin_stale").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaHash\":\"hash_original\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(content).path("data").path("previewToken").asText();

        // 模拟草稿被更新（直接修改数据库中的 schemaHash）
        setup.draft().setSchemaHash("hash_updated_new");
        pageDraftMapper.updateById(setup.draft());

        // 旧 Token 访问 -> 401（哈希不一致）
        mockMvc.perform(get("/portal/api/page-builder/previews/" + token)
                        .with(user("admin_stale").roles("ADMINISTRATOR")))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // 6. 撤销后 Token 失效，再次撤销幂等返回成功
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("撤销后再次撤销同一 Token 幂等返回 200")
    void revokePreview_alreadyRevoked_shouldBeIdempotent() throws Exception {
        PageSetup setup = setupPage("preview_idempotent", "hash_idem");
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, setup.page().getId(), null, "admin_idem", "管理员", false);

        String content = mockMvc.perform(post("/admin/api/page-builder/drafts/" + setup.page().getId() + "/previews")
                        .with(csrf())
                        .with(user("admin_idem").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaHash\":\"hash_idem\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(content).path("data").path("previewToken").asText();

        // 第一次撤销
        mockMvc.perform(delete("/admin/api/page-builder/previews/" + token)
                        .with(csrf())
                        .with(user("admin_idem").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken()))
                .andExpect(status().isOk());

        // 第二次撤销（已撤销/过期）-> 仍然返回 200（幂等）
        mockMvc.perform(delete("/admin/api/page-builder/previews/" + token)
                        .with(csrf())
                        .with(user("admin_idem").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken()))
                .andExpect(status().isOk());
    }

    // -----------------------------------------------------------------------
    // 7. 跨管理员撤销预览 Token -> 403（createdBy 不匹配）
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("非创建管理员撤销他人预览 Token 返回 403")
    void revokePreview_byOtherAdmin_shouldReturn403() throws Exception {
        PageSetup setup = setupPage("preview_revoke_cross", "hash_cross");
        LockStatusVO creatorLock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, setup.page().getId(), null, "creator_revoke", "创建者", false);

        String content = mockMvc.perform(post("/admin/api/page-builder/drafts/" + setup.page().getId() + "/previews")
                        .with(csrf())
                        .with(user("creator_revoke").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", creatorLock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"schemaHash\":\"hash_cross\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(content).path("data").path("previewToken").asText();

        // 创建者主动释放锁，另一个管理员获取锁
        editorLockService.releaseLock(EditorResourceTypeEnum.PAGE, setup.page().getId(),
                creatorLock.getLockToken(), "creator_revoke");
        LockStatusVO otherLock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, setup.page().getId(), null, "other_revoke", "其他管理员", false);

        // 另一个管理员持有有效锁，但不是 Token 创建者 -> 403
        mockMvc.perform(delete("/admin/api/page-builder/previews/" + token)
                        .with(csrf())
                        .with(user("other_revoke").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", otherLock.getLockToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.AUTH_FORBIDDEN.getCode()));
    }
}
