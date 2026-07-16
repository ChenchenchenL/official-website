package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.modules.media.vo.MediaUploadVO;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StageS8ComprehensiveSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EditorLockService editorLockService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("S8 综合收口：页面发布引用强校验、详情下线与回滚强引用拦截、媒体删除 protection 及 409 Conflict 测试")
    void testStageS8CrossReferenceAndCacheProtection() throws Exception {
        // Step 1: 创建未发布的 Product 草稿
        String prodDraftResp = mockMvc.perform(post("/admin/api/products/drafts")
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Long unpubProductId = objectMapper.readTree(prodDraftResp).path("data").path("productId").asLong();

        // 尝试保存未发布 Product 草稿
        LockStatusVO prodLock = editorLockService.acquireLock(EditorResourceTypeEnum.PRODUCT, unpubProductId, null, "admin_s8", "单元测试", true);

        // 先上传一个可用 Logo 媒体
        MockMultipartFile logoFile = new MockMultipartFile("file", "logo.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0});
        String mediaUploadResp = mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(logoFile)
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Long logoMediaId = objectMapper.readTree(mediaUploadResp).path("data").path("mediaId").asLong();

        String prodSaveBody = String.format("{\"version\":0,\"draft\":{\"name\":\"S8测试专属AI产品\",\"logoId\":%d,\"summary\":\"全栈智能解决方案\",\"seo\":{\"title\":\"S8产品\",\"description\":\"SEO描述\"}}}", logoMediaId);
        mockMvc.perform(put("/admin/api/products/" + unpubProductId + "/draft")
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", prodLock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(prodSaveBody))
                .andExpect(status().isOk());

        // Step 2: 创建 Page 并在 Schema 中绑定引用未发布的 Product (unpubProductId)
        String pageCreateResp = mockMvc.perform(post("/admin/api/page-builder/pages")
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pageKey\":\"s8_test_page\",\"name\":\"S8综合测试页\",\"pageType\":\"NORMAL\",\"routePath\":\"/s8-test-page\",\"visible\":true,\"sortOrder\":10}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Long pageId = objectMapper.readTree(pageCreateResp).path("data").path("id").asLong();

        LockStatusVO pageLock = editorLockService.acquireLock(EditorResourceTypeEnum.PAGE, pageId, null, "admin_s8", "页面编辑", true);

        String pageSaveBody = String.format("{\"version\":0,\"schemaJson\":{\"pageKey\":\"s8_test_page\",\"name\":\"S8综合测试页\",\"layout\":{\"type\":\"FLEX\"},\"sections\":[{\"id\":\"sec1\",\"component\":\"ProductGrid\",\"props\":{},\"binding\":{\"mode\":\"ENTITY\",\"source\":\"product\",\"query\":{\"id\":\"%d\"}}}]}}", unpubProductId);
        mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", pageLock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(pageSaveBody))
                .andExpect(status().isOk());

        // 【校验 1】：发布页面时引用未发布产品 -> 被拒绝并返回 10013 错误与 400 状态码
        mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/publish")
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", pageLock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"尝试发布引用未发布产品的页面\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.DETAIL_PUBLISH_VALIDATION_FAILED.getCode()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("未发布上线或已不可见")));

        // Step 3: 发布 Product 上线
        String pubProdResp = mockMvc.perform(post("/admin/api/products/" + unpubProductId + "/publish")
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", prodLock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"正式发布S8测试产品\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn().getResponse().getContentAsString();
        Long prodVersionId = objectMapper.readTree(pubProdResp).path("data").path("id").asLong();

        // 发布 Product 后，再次发布 Page -> 顺利成功生成 ACTIVE 快照
        mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/publish")
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", pageLock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"重新发布页面成功\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        // 【校验 2】：已发布 ACTIVE 页面引用的 Product 下线被强引用拦截 -> 409 Conflict (10010)
        mockMvc.perform(post("/admin/api/products/" + unpubProductId + "/offline")
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", prodLock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"reason\":\"尝试下线被上线页面引用的产品\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_REFERENCE_CONFLICT.getCode()));

        // 【校验 3】：已发布 ACTIVE 页面引用的 Product 回滚被强引用拦截 -> 409 Conflict (10010)
        mockMvc.perform(post("/admin/api/products/" + unpubProductId + "/rollback/" + prodVersionId)
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR"))
                        .header("X-Editor-Lock-Token", prodLock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":1,\"changeSummary\":\"尝试回滚被上线页面引用的产品\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_REFERENCE_CONFLICT.getCode()));

        // 【校验 4】：尝试删除被已发布产品引用的 Logo 媒体 -> 409 Conflict (10010)
        mockMvc.perform(delete("/admin/api/media/assets/" + logoMediaId)
                        .with(csrf())
                        .with(user("admin_s8").roles("ADMINISTRATOR"))
                        .param("version", "1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.RESOURCE_REFERENCE_CONFLICT.getCode()));
    }
}
