package com.company.officialwebsite.modules.product.controller;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.modules.content.entity.ContentReferenceEntity;
import com.company.officialwebsite.modules.content.mapper.ContentReferenceMapper;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionDraftMapper;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionMapper;
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
class AdminIndustrySolutionEditorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EditorLockService editorLockService;

    @Autowired
    private IndustrySolutionMapper solutionMapper;

    @Autowired
    private IndustrySolutionDraftMapper draftMapper;

    @Autowired
    private ContentReferenceMapper contentReferenceMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("行业方案编辑器 12 个 API 全生命周期安全与锁校验及 Portal 隔离测试")
    void testIndustrySolutionEditorFullLifecycle() throws Exception {
        // 1. 创建行业方案草稿外壳 -> 200 OK
        String draftShellResp = mockMvc.perform(post("/admin/api/industry-solutions/drafts")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.solutionId").exists())
                .andReturn().getResponse().getContentAsString();

        Long solutionId = objectMapper.readTree(draftShellResp).path("data").path("solutionId").asLong();

        // 2. 查询草稿 -> 200 OK
        mockMvc.perform(get("/admin/api/industry-solutions/" + solutionId + "/draft")
                        .with(user("admin_sol").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 2.1 未发布前 Portal 侧获取公开详情 -> 提示资源不可见（不泄露后台草稿）
        mockMvc.perform(get("/portal/api/industry-solutions/" + solutionId))
                .andExpect(jsonPath("$.code").value(40101));

        // 3. 无锁保存草稿 -> 400 Bad Request (10011 缺少锁 Header)
        mockMvc.perform(put("/admin/api/industry-solutions/" + solutionId + "/draft")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"draft\":{\"name\":\"智慧金融解决方案\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10011));

        // 获取锁
        LockStatusVO lock = editorLockService.acquireLock(EditorResourceTypeEnum.INDUSTRY_SOLUTION, solutionId, null, "admin_sol", "行业专家", true);

        // 持锁保存草稿 -> 200 OK
        String savedDraftResp = mockMvc.perform(put("/admin/api/industry-solutions/" + solutionId + "/draft")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"draft\":{\"name\":\"智慧金融解决方案\",\"description\":\"面向银行业的全栈数据中台解决方案\"},\"editorSessionRemark\":\"首次编辑方案\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String draftHash = objectMapper.readTree(savedDraftResp).path("data").path("draftHash").asText();

        // 4. 创建预览必须携带匹配的草稿哈希。
        mockMvc.perform(post("/admin/api/industry-solutions/" + solutionId + "/draft/previews")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/admin/api/industry-solutions/" + solutionId + "/draft/previews")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftHash\":\"stale-hash\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(90006));

        // 4. 持锁生成受控预览 Token -> 200 OK
        String tokenResp = mockMvc.perform(post("/admin/api/industry-solutions/" + solutionId + "/draft/previews")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftHash\":\"" + draftHash + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andReturn().getResponse().getContentAsString();

        String previewToken = objectMapper.readTree(tokenResp).path("data").asText();

        // 5. Portal 侧未登录访问预览 -> 401 Unauthorized
        mockMvc.perform(get("/portal/api/industry-solutions/" + solutionId + "/previews/" + previewToken))
                .andExpect(status().isUnauthorized());

        // 5.1 Portal 侧登录创建者访问预览 -> 200 OK with no-store header
        mockMvc.perform(get("/portal/api/industry-solutions/" + solutionId + "/previews/" + previewToken)
                        .with(user("admin_sol").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        mockMvc.perform(get("/portal/api/industry-solutions/" + solutionId + "/previews/" + previewToken)
                        .with(user("other_admin").roles("ADMINISTRATOR")))
                .andExpect(status().isForbidden());

        String updatedDraftResp = mockMvc.perform(put("/admin/api/industry-solutions/" + solutionId + "/draft")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"draft\":{\"name\":\"智慧金融解决方案\",\"description\":\"更新后的金融方案\"}}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String updatedDraftHash = objectMapper.readTree(updatedDraftResp).path("data").path("draftHash").asText();

        mockMvc.perform(get("/portal/api/industry-solutions/" + solutionId + "/previews/" + previewToken)
                        .with(user("admin_sol").roles("ADMINISTRATOR")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(90007));

        String activePreviewResp = mockMvc.perform(post("/admin/api/industry-solutions/" + solutionId + "/draft/previews")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftHash\":\"" + updatedDraftHash + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String activePreviewToken = objectMapper.readTree(activePreviewResp).path("data").asText();

        // 6. 持锁撤销预览 Token -> 200 OK
        mockMvc.perform(delete("/admin/api/industry-solutions/" + solutionId + "/previews/" + activePreviewToken)
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken()))
                .andExpect(status().isOk());

        // 7. 持锁发布草稿 -> 200 OK, versionNo = 1
        String pubResp = mockMvc.perform(post("/admin/api/industry-solutions/" + solutionId + "/publish")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":2,\"changeSummary\":\"发布方案 V1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andReturn().getResponse().getContentAsString();

        Long versionId = objectMapper.readTree(pubResp).path("data").path("id").asLong();

        // 7.1 发布后 Portal 侧正常访问公开详情 -> 200 OK
        mockMvc.perform(get("/portal/api/industry-solutions/" + solutionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").exists());

        // 8. 查询历史版本列表 -> 200 OK
        mockMvc.perform(get("/admin/api/industry-solutions/" + solutionId + "/versions")
                        .with(user("admin_sol").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].versionNo").value(1));

        // 9. 持锁回滚至 versionId -> 200 OK, versionNo = 2, rollbackSourceVersionId = versionId
        mockMvc.perform(post("/admin/api/industry-solutions/" + solutionId + "/rollback/" + versionId)
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":2,\"changeSummary\":\"测试方案回滚\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").value(2))
                .andExpect(jsonPath("$.data.rollbackSourceVersionId").value(versionId));

        // 10. 模拟强引用依赖 -> 下线返回 409 Conflict (10010)
        ContentReferenceEntity ref = new ContentReferenceEntity();
        ref.setReferrerType("PAGE");
        ref.setReferrerKey("fin_solution_page");
        ref.setReferencedType("INDUSTRY_SOLUTION");
        ref.setReferencedId(solutionId);
        ref.setReferenceType("STRONG");
        contentReferenceMapper.insert(ref);

        mockMvc.perform(post("/admin/api/industry-solutions/" + solutionId + "/offline")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":3,\"reason\":\"测试方案下线\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10010));

        // 清理强引用依赖后尝试下线 -> 200 OK
        contentReferenceMapper.deleteById(ref.getId());

        mockMvc.perform(post("/admin/api/industry-solutions/" + solutionId + "/offline")
                        .with(csrf())
                        .with(user("admin_sol").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":3,\"reason\":\"正式下线无强引用行业方案\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 10.1 下线后 Portal 侧再次访问公开详情 -> 资源不可见错误 (40101)
        mockMvc.perform(get("/portal/api/industry-solutions/" + solutionId))
                .andExpect(jsonPath("$.code").value(40101));
    }
}
