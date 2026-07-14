package com.company.officialwebsite.modules.pagebuilder.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PreviewCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * PortalPreviewControllerTest：覆盖受控预览的安全门禁、数据清洗、缓存隔离和 Token 生命周期验收标准。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortalPreviewControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String PAGE_KEY = "preview-test-page";

    @BeforeEach
    void setup() {
        // 清理测试数据
        jdbcTemplate.execute("DELETE FROM cms_page_dependency WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM cms_page_publish_snapshot WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM cms_page_version WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM cms_page_draft WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "'");
        // 清理可能残留的预览 Token
        Set<String> previewKeys = redisTemplate.keys("official:admin:page-preview:*");
        if (previewKeys != null && !previewKeys.isEmpty()) {
            redisTemplate.delete(previewKeys);
        }
    }

    // -----------------------------------------------------------------------
    // [安全门禁] 无效/不存在/篡改 Token 必须被拦截，返回 401
    // -----------------------------------------------------------------------

    @Test
    void getPreview_shouldReturn401_whenTokenNotExist() throws Exception {
        mockMvc.perform(get("/portal/api/page-builder/previews/nonexistent-token-uuid"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED.getCode()));
    }

    @Test
    void getPreview_shouldReturn401_whenTokenIsBlank() throws Exception {
        // 路径参数为空白字符串会被路由为不匹配（404），测试传入极短无效 token
        mockMvc.perform(get("/portal/api/page-builder/previews/x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED.getCode()));
    }

    @Test
    void getPreview_shouldReturn401_afterTokenRevoked() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPageAndSaveDraft(session);

        // 生成 Token
        String token = createPreviewToken(session, pageId);

        // 撤销 Token
        mockMvc.perform(delete("/admin/api/page-builder/previews/" + token)
                        .session(session)
                        .with(csrf()))
                .andExpect(status().isOk());

        // 撤销后访问必须 401
        mockMvc.perform(get("/portal/api/page-builder/previews/" + token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED.getCode()));
    }

    // -----------------------------------------------------------------------
    // [安全门禁] Token 撤销接口需要管理员权限
    // -----------------------------------------------------------------------

    @Test
    void revokePreview_shouldReturn401_whenNotLoggedIn() throws Exception {
        mockMvc.perform(delete("/admin/api/page-builder/previews/some-token")
                        .with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // [数据清洗] 响应中 binding 关键字出现次数必须为 0
    // -----------------------------------------------------------------------

    @Test
    void getPreview_shouldNotContainBinding_inResponse() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPageAndSaveDraftWithBinding(session);
        String token = createPreviewToken(session, pageId);

        String responseBody = mockMvc.perform(get("/portal/api/page-builder/previews/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn().getResponse().getContentAsString();

        // 验收标准：响应体中 binding 关键字出现次数必须为 0
        assertThat(responseBody).doesNotContain("\"binding\"");
        // schemaJson 也不应出现（不是 PagePreviewVO 结构，而是 PortalPageVO 结构）
        assertThat(responseBody).doesNotContain("\"schemaJson\"");
    }

    // -----------------------------------------------------------------------
    // [响应头] Cache-Control: no-store 必须存在
    // -----------------------------------------------------------------------

    @Test
    void getPreview_shouldHaveCacheControlNoStore() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPageAndSaveDraft(session);
        String token = createPreviewToken(session, pageId);

        mockMvc.perform(get("/portal/api/page-builder/previews/" + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    // -----------------------------------------------------------------------
    // [缓存隔离] 预览请求不得产生 official:portal:page:* 正式缓存 Key
    // -----------------------------------------------------------------------

    @Test
    void getPreview_shouldNotPolluteFormalPortalCache() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPageAndSaveDraft(session);
        String token = createPreviewToken(session, pageId);

        // 记录预览前的正式缓存 Key 数量
        Set<String> beforeKeys = redisTemplate.keys("official:portal:page:*");
        int beforeCount = beforeKeys == null ? 0 : beforeKeys.size();

        // 执行多次预览请求
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/portal/api/page-builder/previews/" + token))
                    .andExpect(status().isOk());
        }

        // 正式缓存 Key 数量不得增加
        Set<String> afterKeys = redisTemplate.keys("official:portal:page:*");
        int afterCount = afterKeys == null ? 0 : afterKeys.size();
        assertThat(afterCount).isEqualTo(beforeCount);
    }

    // -----------------------------------------------------------------------
    // [生成预览] Admin 端生成接口完整响应验证
    // -----------------------------------------------------------------------

    @Test
    void createPreview_shouldRequireAdminRole() throws Exception {
        mockMvc.perform(post("/admin/api/page-builder/drafts/1/previews")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createPreview_shouldReturn404_whenDraftSchemaEmpty() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 创建页面但不保存草稿（Schema 为空）
        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey(PAGE_KEY);
        createDTO.setName("预览测试页面");
        createDTO.setRoutePath("/preview-test");
        createDTO.setPageType("NORMAL");
        createDTO.setVisible(true);
        createDTO.setSortOrder(99);

        String createResp = mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long pageId = objectMapper.readTree(createResp).path("data").path("id").asLong();

        mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/previews")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_DRAFT_NOT_FOUND.getCode()));
    }

    @Test
    void createPreview_shouldReturnTokenAndUrl_whenDraftExists() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPageAndSaveDraft(session);

        String resp = mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/previews")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewToken").isString())
                .andExpect(jsonPath("$.data.previewUrl").isString())
                .andExpect(jsonPath("$.data.expiresAt").isString())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(resp).path("data").path("previewToken").asText();
        assertThat(token).isNotBlank();
        // Token 应该是 UUID 格式（含 4 个连字符）
        assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void createPreview_shouldReturn400_whenSchemaHashMismatch() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPageAndSaveDraft(session);

        PreviewCreateDTO dto = new PreviewCreateDTO();
        dto.setSchemaHash("wrong-hash-value");

        mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/previews")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_PREVIEW_SCHEMA_HASH_MISMATCH.getCode()));
    }

    // -----------------------------------------------------------------------
    // [完整预览链路] 草稿内容正确渲染并清洗 binding
    // -----------------------------------------------------------------------

    @Test
    void getPreview_shouldRenderDraftWithBindingDataAndNoBindingField() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPageAndSaveDraftWithBinding(session);
        String token = createPreviewToken(session, pageId);

        String resp = mockMvc.perform(get("/portal/api/page-builder/previews/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageKey").value(PAGE_KEY))
                .andExpect(jsonPath("$.data.sections").isArray())
                .andReturn().getResponse().getContentAsString();

        JsonNode sections = objectMapper.readTree(resp).path("data").path("sections");
        assertThat(sections.size()).isGreaterThan(0);

        for (JsonNode sec : sections) {
            // 每个 section 不得含有 binding 字段
            assertThat(sec.has("binding")).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private long createPageAndSaveDraft(MockHttpSession session) throws Exception {
        return createPageAndSaveDraftInternal(session, false);
    }

    private long createPageAndSaveDraftWithBinding(MockHttpSession session) throws Exception {
        return createPageAndSaveDraftInternal(session, true);
    }

    private long createPageAndSaveDraftInternal(MockHttpSession session, boolean withBinding) throws Exception {
        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey(PAGE_KEY);
        createDTO.setName("预览测试页面");
        createDTO.setRoutePath("/preview-test");
        createDTO.setPageType("NORMAL");
        createDTO.setVisible(true);
        createDTO.setSortOrder(99);

        String createResp = mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long pageId = objectMapper.readTree(createResp).path("data").path("id").asLong();

        PageSchemaModel schema = new PageSchemaModel();
        schema.setPageKey(PAGE_KEY);
        schema.setName("预览测试页面");
        LayoutModel layout = new LayoutModel();
        layout.setType("vertical");
        schema.setLayout(layout);

        SectionModel section = new SectionModel();
        section.setId("sec_1");
        section.setComponent("MetricCards");
        section.setVisible(true);
        Map<String, Object> props = new HashMap<>();
        props.put("title", "预览标题");
        section.setProps(props);

        if (withBinding) {
            BindingModel binding = new BindingModel();
            binding.setMode("ENTITY");
            binding.setSource("site_config");
            section.setBinding(binding);
        }

        schema.setSections(Collections.singletonList(section));

        PageDraftSaveDTO saveDTO = new PageDraftSaveDTO();
        saveDTO.setSchemaJson(schema);
        saveDTO.setEditorSessionRemark("测试草稿");
        saveDTO.setVersion(0);

        mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk());

        return pageId;
    }

    private String createPreviewToken(MockHttpSession session, long pageId) throws Exception {
        String resp = mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/previews")
                        .session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resp).path("data").path("previewToken").asText();
    }
}
