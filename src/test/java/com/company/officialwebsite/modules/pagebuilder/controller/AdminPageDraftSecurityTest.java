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
}
