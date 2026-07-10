package com.company.officialwebsite.modules.pagebuilder.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PagePublishDTO;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * PortalPageRenderControllerTest: 前台页面渲染与元数据提取接口集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortalPageRenderControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearCacheAndDatabase() {
        redisTemplate.delete(java.util.Arrays.asList(
                "official:portal:page:/careers",
                "official:portal:page-meta:careers-page"
        ));
        jdbcTemplate.execute("DELETE FROM cms_page_dependency WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'careers-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_publish_snapshot WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'careers-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_version WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'careers-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_draft WHERE page_id IN (SELECT id FROM cms_page_definition WHERE page_key = 'careers-page')");
        jdbcTemplate.execute("DELETE FROM cms_page_definition WHERE page_key = 'careers-page'");
    }

    @Test
    void getPageRender_shouldReturn404_whenPageNotExists() throws Exception {
        mockMvc.perform(get("/portal/api/page-builder/pages")
                        .param("routePath", "/invalid-path-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_NOT_FOUND.getCode()));
    }

    @Test
    void getPageMeta_shouldReturn404_whenPageNotExists() throws Exception {
        mockMvc.perform(get("/portal/api/page-builder/pages/invalid-key-999/meta"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_NOT_FOUND.getCode()));
    }

    @Test
    void fullRenderAndMetaWorkflow_shouldSucceed() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 1. 创建页面定义
        PageDefinitionCreateDTO createDTO = new PageDefinitionCreateDTO();
        createDTO.setPageKey("careers-page");
        createDTO.setName("招贤纳士页面");
        createDTO.setRoutePath("/careers");
        createDTO.setPageType("NORMAL");
        createDTO.setVisible(true);
        createDTO.setSortOrder(60);

        String createResponse = mockMvc.perform(post("/admin/api/page-builder/pages")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDTO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        long pageId = objectMapper.readTree(createResponse).path("data").path("id").asLong();

        // 2. 保存草稿（添加一个绑定 site_config 数据源的区块）
        PageSchemaModel schema = new PageSchemaModel();
        schema.setPageKey("careers-page");
        schema.setName("招贤纳士页面");
        
        LayoutModel layout = new LayoutModel();
        layout.setType("horizontal");
        schema.setLayout(layout);

        SectionModel section = new SectionModel();
        section.setId("careers_sec_1");
        section.setComponent("MetricCards");
        section.setVisible(true);
        Map<String, Object> props = new HashMap<>();
        props.put("title", "加入我们");
        section.setProps(props);

        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("site_config"); // 绑定站点全局配置
        section.setBinding(binding);

        schema.setSections(Collections.singletonList(section));

        PageDraftSaveDTO saveDTO = new PageDraftSaveDTO();
        saveDTO.setSchemaJson(schema);
        saveDTO.setEditorSessionRemark("招贤纳士草稿");
        saveDTO.setVersion(0);

        mockMvc.perform(put("/admin/api/page-builder/drafts/" + pageId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(saveDTO)))
                .andExpect(status().isOk());

        // 3. 此时未发布，前台直接查询应该返回 404
        mockMvc.perform(get("/portal/api/page-builder/pages")
                        .param("routePath", "/careers"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(ErrorCode.PAGE_NOT_FOUND.getCode()));

        // 4. 发布草稿
        PagePublishDTO publishDTO = new PagePublishDTO();
        publishDTO.setChangeSummary("发布招聘页面");

        mockMvc.perform(post("/admin/api/page-builder/pages/" + pageId + "/publish")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(publishDTO)))
                .andExpect(status().isOk());

        // 5. 发布后前台直接渲染（免密，且应该成功装配 site_config 绑定数据）
        mockMvc.perform(get("/portal/api/page-builder/pages")
                        .param("routePath", "/careers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.pageKey").value("careers-page"))
                .andExpect(jsonPath("$.data.sections[0].bindingData").exists())
                .andExpect(jsonPath("$.data.sections[0].bindingData.siteTitle").isString());

        // 6. 前台获取元数据页面信息
        mockMvc.perform(get("/portal/api/page-builder/pages/careers-page/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.pageKey").value("careers-page"))
                .andExpect(jsonPath("$.data.layout.type").value("horizontal"))
                .andExpect(jsonPath("$.data.sections").doesNotExist()); // 确保元数据接口不泄漏 sections 信息
    }
}
