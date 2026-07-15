package com.company.officialwebsite.modules.product.controller;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.modules.content.entity.ContentReferenceEntity;
import com.company.officialwebsite.modules.content.mapper.ContentReferenceMapper;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionEntity;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionMapper;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.company.officialwebsite.modules.product.mapper.ProductDraftMapper;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
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
class AdminProductEditorSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EditorLockService editorLockService;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private ProductDraftMapper draftMapper;

    @Autowired
    private ContentReferenceMapper contentReferenceMapper;

    @Autowired
    private CaseMapper caseMapper;

    @Autowired
    private IndustrySolutionMapper industrySolutionMapper;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("产品详情编辑器 10 个 API 全生命周期安全与锁校验流程测试")
    void testProductEditorFullLifecycle() throws Exception {
        // 1. 创建产品草稿外壳 -> 200 OK
        String draftShellResp = mockMvc.perform(post("/admin/api/products/drafts")
                        .with(csrf())
                        .with(user("admin_prod").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.productId").exists())
                .andReturn().getResponse().getContentAsString();

        Long productId = objectMapper.readTree(draftShellResp).path("data").path("productId").asLong();

        // 2. 查询草稿 -> 200 OK
        mockMvc.perform(get("/admin/api/products/" + productId + "/draft")
                        .with(user("admin_prod").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 3. 无锁保存草稿 -> 400 Bad Request (10011 缺少锁 Header)
        mockMvc.perform(put("/admin/api/products/" + productId + "/draft")
                        .with(csrf())
                        .with(user("admin_prod").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"draft\":{\"name\":\"智能数据平台\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(10011));

        // 获取锁
        LockStatusVO lock = editorLockService.acquireLock(EditorResourceTypeEnum.PRODUCT, productId, null, "admin_prod", "产品经理", true);

        // 持锁保存草稿 -> 200 OK
        String draftResponse = mockMvc.perform(put("/admin/api/products/" + productId + "/draft")
                        .with(csrf())
                        .with(user("admin_prod").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"draft\":{\"name\":\"智能数据平台\",\"subTitle\":\"核心数据引擎\"},\"editorSessionRemark\":\"首次保存\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        String draftHash = objectMapper.readTree(draftResponse).path("data").path("draftHash").asText();

        // 4. 持锁生成受控预览 Token -> 200 OK
        String tokenResp = mockMvc.perform(post("/admin/api/products/" + productId + "/draft/previews")
                        .with(csrf())
                        .with(user("admin_prod").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"draftHash\":\"" + draftHash + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").exists())
                .andReturn().getResponse().getContentAsString();

        String previewToken = objectMapper.readTree(tokenResp).path("data").asText();

        // 5. Portal 侧未登录访问预览 -> 401 Unauthorized
        mockMvc.perform(get("/portal/api/products/" + productId + "/previews/" + previewToken))
                .andExpect(status().isUnauthorized());

        // 5.1 Portal 侧登录创建者访问预览 -> 200 OK with no-store header
        mockMvc.perform(get("/portal/api/products/" + productId + "/previews/" + previewToken)
                        .with(user("admin_prod").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));

        // 6. 持锁撤销预览 Token -> 200 OK
        mockMvc.perform(delete("/admin/api/products/" + productId + "/previews/" + previewToken)
                        .with(csrf())
                        .with(user("admin_prod").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken()))
                .andExpect(status().isOk());

        // 7. 持锁发布草稿 -> 200 OK, versionNo = 1
        String pubResp = mockMvc.perform(post("/admin/api/products/" + productId + "/publish")
                        .with(csrf())
                        .with(user("admin_prod").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"正式发布 V1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").value(1))
                .andReturn().getResponse().getContentAsString();

        Long versionId = objectMapper.readTree(pubResp).path("data").path("id").asLong();

        // 8. 查询历史版本列表 -> 200 OK
        mockMvc.perform(get("/admin/api/products/" + productId + "/versions")
                        .with(user("admin_prod").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].versionNo").value(1));

        // 9. 持锁回滚至 versionId -> 200 OK, versionNo = 2, rollbackSourceVersionId = versionId
        mockMvc.perform(post("/admin/api/products/" + productId + "/rollback/" + versionId)
                        .with(csrf())
                        .with(user("admin_prod").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"测试回滚操作\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.versionNo").value(2))
                .andExpect(jsonPath("$.data.rollbackSourceVersionId").value(versionId));

        // 10. 模拟强引用依赖 -> 下线返回 409 Conflict (10010)
        ContentReferenceEntity ref = new ContentReferenceEntity();
        ref.setReferrerType("PAGE");
        ref.setReferrerKey("home");
        ref.setReferencedType("PRODUCT");
        ref.setReferencedId(productId);
        ref.setReferenceType("STRONG");
        contentReferenceMapper.insert(ref);

        mockMvc.perform(post("/admin/api/products/" + productId + "/offline")
                        .with(csrf())
                        .with(user("admin_prod").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":2,\"reason\":\"测试下线\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(10010));

        // 清理强引用依赖后尝试下线 -> 200 OK
        contentReferenceMapper.deleteById(ref.getId());

        mockMvc.perform(post("/admin/api/products/" + productId + "/offline")
                        .with(csrf())
                        .with(user("admin_prod").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":2,\"reason\":\"正式下线无强引用产品\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("Portal 产品详情完整读取发布快照正文、SEO 与关联实体")
    void portalDetail_shouldRenderPublishedSnapshotFieldsAndRelations() throws Exception {
        Long productId = createDraftShell();
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PRODUCT, productId, null, "snapshot_admin", "产品经理", true);

        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setTitle("快照关联案例");
        caseEntity.setLogoMediaId(0L);
        caseEntity.setSummary("案例摘要");
        caseEntity.setVisible(true);
        caseEntity.setStatus("PUBLISHED");
        caseEntity.setSortOrder(1);
        caseMapper.insert(caseEntity);

        IndustrySolutionEntity solution = new IndustrySolutionEntity();
        solution.setName("快照关联方案");
        solution.setIconMediaId(0L);
        solution.setDescription("方案描述");
        solution.setVisible(true);
        solution.setSortOrder(1);
        industrySolutionMapper.insert(solution);

        String draft = "{\"name\":\"快照标题\",\"content\":\"<p>发布正文</p>\","
                + "\"seo\":{\"title\":\"快照SEO标题\",\"description\":\"快照SEO描述\"},"
                + "\"caseIds\":[" + caseEntity.getId() + "],\"industrySolutionIds\":[" + solution.getId() + "]}";
        mockMvc.perform(put("/admin/api/products/" + productId + "/draft")
                        .with(csrf()).with(user("snapshot_admin").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"draft\":" + draft + "}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/admin/api/products/" + productId + "/publish")
                        .with(csrf()).with(user("snapshot_admin").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"发布完整快照\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/portal/api/products/" + productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("<p>发布正文</p>"))
                .andExpect(jsonPath("$.data.seoTitle").value("快照SEO标题"))
                .andExpect(jsonPath("$.data.seoDescription").value("快照SEO描述"))
                .andExpect(jsonPath("$.data.relatedCases[0].id").value(caseEntity.getId()))
                .andExpect(jsonPath("$.data.relatedIndustrySolutions[0].id").value(solution.getId()));
    }

    @Test
    @DisplayName("保存草稿拒绝非法媒体、案例和行业方案 ID 数组")
    void saveDraft_shouldRejectInvalidMediaAndRelationIdArrays() throws Exception {
        Long productId = createDraftShell();
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PRODUCT, productId, null, "validation_admin", "产品经理", true);

        assertDraftRejected(productId, lock, "{\"mediaIds\":[999999]}", 60001);
        assertDraftRejected(productId, lock, "{\"caseIds\":[999999]}", 40201);
        assertDraftRejected(productId, lock, "{\"industrySolutionIds\":[999999]}", 40101);
    }

    private Long createDraftShell() throws Exception {
        String response = mockMvc.perform(post("/admin/api/products/drafts")
                        .with(csrf()).with(user("fixture_admin").roles("ADMINISTRATOR")))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).path("data").path("productId").asLong();
    }

    private void assertDraftRejected(Long productId, LockStatusVO lock, String draft, int errorCode) throws Exception {
        mockMvc.perform(put("/admin/api/products/" + productId + "/draft")
                        .with(csrf()).with(user("validation_admin").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0,\"draft\":" + draft + "}"))
                .andExpect(jsonPath("$.code").value(errorCode));
    }
}
