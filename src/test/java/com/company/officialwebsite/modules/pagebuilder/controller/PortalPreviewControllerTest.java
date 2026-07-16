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

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PreviewCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
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

    @Autowired
    private EditorLockService editorLockService;

    private static final String PAGE_KEY = "preview-test-page";

    @BeforeEach
    void setup() {
        jdbcTemplate.execute("DELETE FROM cms_page_dependency WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM cms_page_publish_snapshot WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM cms_page_version WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM cms_page_draft WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "')");
        jdbcTemplate.execute("DELETE FROM cms_page_definition WHERE page_key = '" + PAGE_KEY + "'");
        Set<String> previewKeys = redisTemplate.keys("official:admin:page-preview:*");
        if (previewKeys != null && !previewKeys.isEmpty()) {
            redisTemplate.delete(previewKeys);
        }
    }

    // -----------------------------------------------------------------------
    // [安全门禁] 无效/不存在/篡改 Token 必须被拦截，返回 401
    // 注：portal preview GET 要求管理员身份，需携带 session 才能到达 Token 校验层
    // -----------------------------------------------------------------------

    @Test
    void getPreview_shouldReturn401_whenTokenNotExist() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/portal/api/page-builder/previews/nonexistent-token-uuid")
                        .session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED.getCode()));
    }

    @Test
    void getPreview_shouldReturn401_whenTokenIsBlank() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(get("/portal/api/page-builder/previews/x")
                        .session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED.getCode()));
    }

    @Test
    void getPreview_shouldReturn401_afterTokenRevoked() throws Exception {
        MockHttpSession session = loginAsAdmin();

        long pageId = createPage(session);
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);
        String schemaHash = saveDraft(session, pageId, lock.getLockToken(), false);
        String token = createPreviewToken(session, pageId, lock.getLockToken(), schemaHash);

        // 撤销 Token
        mockMvc.perform(delete("/admin/api/page-builder/previews/" + token)
                        .session(session)
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken()))
                .andExpect(status().isOk());

        // 撤销后访问必须 401
        mockMvc.perform(get("/portal/api/page-builder/previews/" + token)
                        .session(session))
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
        long pageId = createPage(session);
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);
        String schemaHash = saveDraft(session, pageId, lock.getLockToken(), true);
        String token = createPreviewToken(session, pageId, lock.getLockToken(), schemaHash);

        String responseBody = mockMvc.perform(get("/portal/api/page-builder/previews/" + token)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn().getResponse().getContentAsString();

        assertThat(responseBody).doesNotContain("\"binding\"");
        assertThat(responseBody).doesNotContain("\"schemaJson\"");
    }

    // -----------------------------------------------------------------------
    // [响应头] Cache-Control: no-store 必须存在
    // -----------------------------------------------------------------------

    @Test
    void getPreview_shouldHaveCacheControlNoStore() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPage(session);
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);
        String schemaHash = saveDraft(session, pageId, lock.getLockToken(), false);
        String token = createPreviewToken(session, pageId, lock.getLockToken(), schemaHash);

        mockMvc.perform(get("/portal/api/page-builder/previews/" + token)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    // -----------------------------------------------------------------------
    // [缓存隔离] 预览请求不得产生 official:portal:page:* 正式缓存 Key
    // -----------------------------------------------------------------------

    @Test
    void getPreview_shouldNotPolluteFormalPortalCache() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPage(session);
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);
        String schemaHash = saveDraft(session, pageId, lock.getLockToken(), false);
        String token = createPreviewToken(session, pageId, lock.getLockToken(), schemaHash);

        Set<String> beforeKeys = redisTemplate.keys("official:portal:page:*");
        int beforeCount = beforeKeys == null ? 0 : beforeKeys.size();

        for (int i = 0; i < 3; i++) {
            mockMvc.perform(get("/portal/api/page-builder/previews/" + token)
                            .session(session))
                    .andExpect(status().isOk());
        }

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
        long pageId = createPage(session);
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);

        // 传入任意 schemaHash（草稿不存在时锁校验通过但草稿不存在返回 404）
        PreviewCreateDTO dto = new PreviewCreateDTO();
        dto.setSchemaHash("placeholder-hash-for-empty-draft");

        mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/previews")
                        .session(session).with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_DRAFT_NOT_FOUND.getCode()));
    }

    @Test
    void createPreview_shouldReturnTokenAndUrl_whenDraftExists() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPage(session);
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);
        String schemaHash = saveDraft(session, pageId, lock.getLockToken(), false);

        PreviewCreateDTO previewReqDTO = new PreviewCreateDTO();
        previewReqDTO.setSchemaHash(schemaHash);

        String resp = mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/previews")
                        .session(session).with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(previewReqDTO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.previewToken").isString())
                .andExpect(jsonPath("$.data.previewUrl").isString())
                .andExpect(jsonPath("$.data.expiresAt").isString())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(resp).path("data").path("previewToken").asText();
        assertThat(token).isNotBlank();
        assertThat(token).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void createPreview_shouldReturn400_whenSchemaHashMismatch() throws Exception {
        MockHttpSession session = loginAsAdmin();
        long pageId = createPage(session);
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);
        saveDraft(session, pageId, lock.getLockToken(), false);

        PreviewCreateDTO dto = new PreviewCreateDTO();
        dto.setSchemaHash("wrong-hash-value");

        mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/previews")
                        .session(session).with(csrf())
                        .header("X-Editor-Lock-Token", lock.getLockToken())
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
        long pageId = createPage(session);
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, pageId, null, TestConstants.ADMIN_USERNAME, "管理员", false);
        String schemaHash = saveDraft(session, pageId, lock.getLockToken(), true);
        String token = createPreviewToken(session, pageId, lock.getLockToken(), schemaHash);

        String resp = mockMvc.perform(get("/portal/api/page-builder/previews/" + token)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageKey").value(PAGE_KEY))
                .andExpect(jsonPath("$.data.sections").isArray())
                .andReturn().getResponse().getContentAsString();

        JsonNode sections = objectMapper.readTree(resp).path("data").path("sections");
        assertThat(sections.size()).isGreaterThan(0);
        for (JsonNode sec : sections) {
            assertThat(sec.has("binding")).isFalse();
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** 创建页面定义，返回 pageId。不涉及草稿保存和锁操作。 */
    private long createPage(MockHttpSession session) throws Exception {
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

        return objectMapper.readTree(createResp).path("data").path("id").asLong();
    }

    /**
     * 保存草稿，返回服务端计算的 schemaHash（供 createPreviewToken 使用）。
     * 调用方需事先持有锁并传入 lockToken。
     */
    private String saveDraft(MockHttpSession session, long pageId, String lockToken, boolean withBinding) throws Exception {
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

        String resp = mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session).with(csrf())
                        .header("X-Editor-Lock-Token", lockToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // 从保存响应中提取服务端计算的 schemaHash，后续 createPreview 必须传入一致的哈希
        return objectMapper.readTree(resp).path("data").path("schemaHash").asText();
    }

    /**
     * 使用已持有的锁 Token 和草稿哈希创建预览，返回 previewToken。
     * 调用方需事先持有锁并通过 saveDraft 获得 schemaHash。
     */
    private String createPreviewToken(MockHttpSession session, long pageId, String lockToken, String schemaHash) throws Exception {
        PreviewCreateDTO dto = new PreviewCreateDTO();
        dto.setSchemaHash(schemaHash);
        String resp = mockMvc.perform(post("/admin/api/page-builder/drafts/" + pageId + "/previews")
                        .session(session).with(csrf())
                        .header("X-Editor-Lock-Token", lockToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(resp).path("data").path("previewToken").asText();
    }
}
