package com.company.officialwebsite.modules.casecenter.controller;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.modules.casecenter.mapper.CaseDraftMapper;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.content.entity.ContentReferenceEntity;
import com.company.officialwebsite.modules.content.mapper.ContentReferenceMapper;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminCaseEditorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EditorLockService editorLockService;

    @Autowired
    private CaseMapper caseMapper;

    @Autowired
    private CaseDraftMapper draftMapper;

    @Autowired
    private ContentReferenceMapper contentReferenceMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("标杆案例编辑器 10 个 API 全生命周期安全与锁校验流程测试")
    void testCaseEditorFullLifecycle() throws Exception {
        // 1. 创建案例草稿外壳 -> 200 OK
        String draftShellResp = mockMvc.perform(post("/admin/api/cases/drafts")
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.caseId").exists())
                .andReturn().getResponse().getContentAsString();

        Long caseId = objectMapper.readTree(draftShellResp).path("data").path("caseId").asLong();

        // 2. 查询草稿 -> 200 OK
        mockMvc.perform(get("/admin/api/cases/" + caseId + "/draft")
                        .with(user("admin_case").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 3. 无锁保存草稿 -> 400 Bad Request (10011 缺少锁 Header)
        mockMvc.perform(put("/admin/api/cases/" + caseId + "/draft")
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"draft\":{\"title\":\"某知名央企数字化转型\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10011));

        // 获取锁
        LockStatusVO lock = editorLockService.acquireLock(EditorResourceTypeEnum.CASE, caseId, null, "admin_case", "案例运营", true);

        // 持锁保存草稿 -> 200 OK
        mockMvc.perform(put("/admin/api/cases/" + caseId + "/draft")
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"draft\":{\"title\":\"某知名央企数字化转型\",\"summary\":\"打造全国领先的数据中台\",\"content\":\"<p>发布快照正文</p>\",\"seo\":{\"title\":\"案例 SEO 标题\",\"description\":\"案例 SEO 描述\"}},\"editorSessionRemark\":\"首次完善案例\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();

        String draftResponse = mockMvc.perform(get("/admin/api/cases/" + caseId + "/draft")
                        .with(user("admin_case").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String draftHash = objectMapper.readTree(draftResponse).path("data").path("draftHash").asText();

        mockMvc.perform(post("/admin/api/cases/" + caseId + "/draft/previews")
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        // 4. 持锁生成受控预览 Token -> 200 OK
        String tokenResp = mockMvc.perform(post("/admin/api/cases/" + caseId + "/draft/previews")
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftHash\":\"" + draftHash + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andReturn().getResponse().getContentAsString();

        String previewToken = objectMapper.readTree(tokenResp).path("data").asText();

        // 5. Portal 侧未登录访问预览 -> 401 Unauthorized
        mockMvc.perform(get("/portal/api/cases/" + caseId + "/previews/" + previewToken))
                .andExpect(status().isUnauthorized());

        // 5.1 Portal 侧登录创建者访问预览 -> 200 OK with no-store header
        mockMvc.perform(get("/portal/api/cases/" + caseId + "/previews/" + previewToken)
                        .with(user("admin_case").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        // 6. 持锁撤销预览 Token -> 200 OK
        mockMvc.perform(delete("/admin/api/cases/" + caseId + "/previews/" + previewToken)
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken()))
                .andExpect(status().isOk());

        // 7. 持锁发布草稿 -> 200 OK, versionNo = 1
        String pubResp = mockMvc.perform(post("/admin/api/cases/" + caseId + "/publish")
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"发布案例首个正式版本\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andReturn().getResponse().getContentAsString();

        Long versionId = objectMapper.readTree(pubResp).path("data").path("id").asLong();

        mockMvc.perform(get("/portal/api/cases/" + caseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("<p>发布快照正文</p>"))
                .andExpect(jsonPath("$.data.seoTitle").value("案例 SEO 标题"))
                .andExpect(jsonPath("$.data.seoDescription").value("案例 SEO 描述"));

        // 8. 查询历史版本列表 -> 200 OK
        mockMvc.perform(get("/admin/api/cases/" + caseId + "/versions")
                        .with(user("admin_case").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].versionNo").value(1));

        // 9. 持锁回滚至 versionId -> 200 OK, versionNo = 2, rollbackSourceVersionId = versionId
        mockMvc.perform(post("/admin/api/cases/" + caseId + "/rollback/" + versionId)
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"测试案例回滚\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").value(2))
                .andExpect(jsonPath("$.data.rollbackSourceVersionId").value(versionId));

        // 10. 模拟强引用依赖 -> 下线返回 409 Conflict (10010)
        ContentReferenceEntity ref = new ContentReferenceEntity();
        ref.setReferrerType("PAGE");
        ref.setReferrerKey("case_study_detail");
        ref.setReferencedType("CASE");
        ref.setReferencedId(caseId);
        ref.setReferenceType("STRONG");
        contentReferenceMapper.insert(ref);

        mockMvc.perform(post("/admin/api/cases/" + caseId + "/offline")
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":2,\"reason\":\"测试案例下线\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10010));

        // 清理强引用依赖后尝试下线 -> 200 OK
        contentReferenceMapper.deleteById(ref.getId());

        mockMvc.perform(post("/admin/api/cases/" + caseId + "/offline")
                        .with(csrf())
                        .with(user("admin_case").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":2,\"reason\":\"正式下线无强引用案例\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("案例预览必须绑定草稿哈希和创建管理员")
    void preview_shouldRejectMismatchedHashNonCreatorAndStaleToken() throws Exception {
        Long caseId = createCaseDraftShell("preview_owner");
        LockStatusVO lock = acquireCaseLock(caseId, "preview_owner");
        saveDraft(caseId, lock.getLockToken(), "preview_owner", 0,
                "{\"title\":\"预览案例\",\"content\":\"<p>V1</p>\"}");
        String draftHash = getDraftHash(caseId, "preview_owner");

        mockMvc.perform(post("/admin/api/cases/" + caseId + "/draft/previews")
                        .with(csrf()).with(user("preview_owner").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftHash\":\"invalid\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90006));

        String previewResponse = mockMvc.perform(post("/admin/api/cases/" + caseId + "/draft/previews")
                        .with(csrf()).with(user("preview_owner").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftHash\":\"" + draftHash + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String previewToken = objectMapper.readTree(previewResponse).path("data").asText();

        mockMvc.perform(get("/portal/api/cases/" + caseId + "/previews/" + previewToken)
                        .with(user("another_admin").roles("ADMINISTRATOR")))
                .andExpect(status().isForbidden());

        saveDraft(caseId, lock.getLockToken(), "preview_owner", 1,
                "{\"title\":\"预览案例\",\"content\":\"<p>V2</p>\"}");
        mockMvc.perform(get("/portal/api/cases/" + caseId + "/previews/" + previewToken)
                        .with(user("preview_owner").roles("ADMINISTRATOR")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("案例发布回滚必须校验锁、版本与强引用")
    void publishAndRollback_shouldRejectWrongLockStaleVersionAndReference() throws Exception {
        Long caseId = createCaseDraftShell("version_owner");
        LockStatusVO lock = acquireCaseLock(caseId, "version_owner");
        saveDraft(caseId, lock.getLockToken(), "version_owner", 0, "{\"title\":\"并发案例\"}");

        mockMvc.perform(post("/admin/api/cases/" + caseId + "/publish")
                        .with(csrf()).with(user("version_owner").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", "invalid-lock-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"错误锁\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(10009));

        mockMvc.perform(post("/admin/api/cases/" + caseId + "/publish")
                        .with(csrf()).with(user("version_owner").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"changeSummary\":\"过期版本\"}"))
                .andExpect(status().isConflict());

        String publishResponse = mockMvc.perform(post("/admin/api/cases/" + caseId + "/publish")
                        .with(csrf()).with(user("version_owner").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"发布 V1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long versionId = objectMapper.readTree(publishResponse).path("data").path("id").asLong();

        mockMvc.perform(post("/admin/api/cases/" + caseId + "/rollback/" + versionId)
                        .with(csrf()).with(user("version_owner").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"changeSummary\":\"过期回滚\"}"))
                .andExpect(status().isConflict());

        ContentReferenceEntity reference = new ContentReferenceEntity();
        reference.setReferrerType("PAGE");
        reference.setReferrerKey("case-detail");
        reference.setReferencedType("CASE");
        reference.setReferencedId(caseId);
        reference.setReferenceType("STRONG");
        contentReferenceMapper.insert(reference);
        mockMvc.perform(post("/admin/api/cases/" + caseId + "/rollback/" + versionId)
                        .with(csrf()).with(user("version_owner").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"被引用回滚\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10010));
    }

    @Test
    @DisplayName("案例草稿必须清洗 XSS 并拒绝非法媒体、链接和关联数组")
    void saveDraft_shouldSanitizeAndRejectInvalidReferences() throws Exception {
        Long caseId = createCaseDraftShell("validation_owner");
        LockStatusVO lock = acquireCaseLock(caseId, "validation_owner");
        saveDraft(caseId, lock.getLockToken(), "validation_owner", 0,
                "{\"title\":\"校验案例\",\"content\":\"<script>alert(1)</script><p>安全正文</p>\"}");
        mockMvc.perform(get("/admin/api/cases/" + caseId + "/draft")
                        .with(user("validation_owner").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.draftJson.content").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("script"))));

        assertInvalidDraft(caseId, lock.getLockToken(), "validation_owner", 1,
                "{\"title\":\"校验案例\",\"imageIds\":[999999]}" );
        assertInvalidDraft(caseId, lock.getLockToken(), "validation_owner", 1,
                "{\"title\":\"校验案例\",\"detailLink\":\"javascript:alert(1)\"}" );
        assertInvalidDraft(caseId, lock.getLockToken(), "validation_owner", 1,
                "{\"title\":\"校验案例\",\"productIds\":[999999]}" );
        assertInvalidDraft(caseId, lock.getLockToken(), "validation_owner", 1,
                "{\"title\":\"校验案例\",\"recommendedCaseIds\":[999999]}" );
    }

    private Long createCaseDraftShell(String username) throws Exception {
        String response = mockMvc.perform(post("/admin/api/cases/drafts")
                        .with(csrf()).with(user(username).roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("caseId").asLong();
    }

    private LockStatusVO acquireCaseLock(Long caseId, String username) {
        return editorLockService.acquireLock(EditorResourceTypeEnum.CASE, caseId, null, username, "案例运营", true);
    }

    private void saveDraft(Long caseId, String lockToken, String username, int version, String draftJson) throws Exception {
        mockMvc.perform(put("/admin/api/cases/" + caseId + "/draft")
                        .with(csrf()).with(user(username).roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lockToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + version + ",\"draft\":" + draftJson + "}"))
                .andExpect(status().isOk());
    }

    private String getDraftHash(Long caseId, String username) throws Exception {
        String response = mockMvc.perform(get("/admin/api/cases/" + caseId + "/draft")
                        .with(user(username).roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("draftHash").asText();
    }

    private void assertInvalidDraft(Long caseId, String lockToken, String username, int version, String draftJson) throws Exception {
        mockMvc.perform(put("/admin/api/cases/" + caseId + "/draft")
                        .with(csrf()).with(user(username).roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lockToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":" + version + ",\"draft\":" + draftJson + "}"))
                .andExpect(status().is4xxClientError());
    }
}
