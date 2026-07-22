package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.PageStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PortalPageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PageDefinitionMapper pageDefinitionMapper;

    @Test
    @DisplayName("Portal 路由清单接口仅返回 ENABLED 页面，且支持 onlyVisible 过滤")
    void listActiveRoutes_shouldFilterDisabledAndSupportOnlyVisible() throws Exception {
        // 1. 插入一个 ENABLED + visible=true 的页面
        PageDefinitionEntity p1 = new PageDefinitionEntity();
        p1.setPageKey("route_test_enabled_visible");
        p1.setName("启用且公开页面");
        p1.setRoutePath("/route-test-1");
        p1.setPageType("NORMAL");
        p1.setStatus(PageStatusEnum.ENABLED.name());
        p1.setVisible(true);
        p1.setSortOrder(1);
        pageDefinitionMapper.insert(p1);

        // 2. 插入一个 ENABLED + visible=false 的隐藏落地页
        PageDefinitionEntity p2 = new PageDefinitionEntity();
        p2.setPageKey("route_test_enabled_hidden");
        p2.setName("启用但隐藏页面");
        p2.setRoutePath("/route-test-2");
        p2.setPageType("NORMAL");
        p2.setStatus(PageStatusEnum.ENABLED.name());
        p2.setVisible(false);
        p2.setSortOrder(2);
        pageDefinitionMapper.insert(p2);

        // 3. 插入一个 DISABLED 的停用页面
        PageDefinitionEntity p3 = new PageDefinitionEntity();
        p3.setPageKey("route_test_disabled");
        p3.setName("停用页面");
        p3.setRoutePath("/route-test-3");
        p3.setPageType("NORMAL");
        p3.setStatus(PageStatusEnum.DISABLED.name());
        p3.setVisible(true);
        p3.setSortOrder(3);
        pageDefinitionMapper.insert(p3);

        // 查询全部 enabled 页面（onlyVisible=false），应该包含 p1 与 p2，不包含 p3
        mockMvc.perform(get("/portal/api/page-builder/pages/routes?onlyVisible=false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.pageKey=='route_test_enabled_visible')]").exists())
                .andExpect(jsonPath("$.data[?(@.pageKey=='route_test_enabled_hidden')]").exists())
                .andExpect(jsonPath("$.data[?(@.pageKey=='route_test_disabled')]").doesNotExist());

        // 查询仅公开页面（onlyVisible=true），应该仅包含 p1，不包含 p2 与 p3
        mockMvc.perform(get("/portal/api/page-builder/pages/routes?onlyVisible=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.pageKey=='route_test_enabled_visible')]").exists())
                .andExpect(jsonPath("$.data[?(@.pageKey=='route_test_enabled_hidden')]").doesNotExist())
                .andExpect(jsonPath("$.data[?(@.pageKey=='route_test_disabled')]").doesNotExist());
    }

    @Autowired
    private com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper pagePublishSnapshotMapper;

    @Test
    @DisplayName("已启用但隐藏的页面 (status=ENABLED, visible=false) 通过直接 URL 仍能成功渲染")
    void getPageRender_hiddenPageWithEnabledStatus_shouldRenderSuccessfully() throws Exception {
        PageDefinitionEntity p = new PageDefinitionEntity();
        p.setPageKey("hidden_landing_page");
        p.setName("隐藏营销落地页");
        p.setRoutePath("/hidden-landing");
        p.setPageType("NORMAL");
        p.setStatus(PageStatusEnum.ENABLED.name());
        p.setVisible(false); // 隐藏页面
        p.setSortOrder(10);
        pageDefinitionMapper.insert(p);

        com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel schema =
                new com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel();
        schema.setPageKey("hidden_landing_page");
        schema.setName("隐藏营销落地页");

        com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity snapshot =
                new com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity();
        snapshot.setPageId(p.getId());
        snapshot.setVersionId(100L);
        snapshot.setPublishStatus(com.company.officialwebsite.modules.pagebuilder.enums.PublishSnapshotStatusEnum.ACTIVE.name());
        snapshot.setSnapshotJson(schema);
        snapshot.setSnapshotHash("hash_hidden_100");
        pagePublishSnapshotMapper.insert(snapshot);

        // 通过直接 URL (/hidden-landing) 发起渲染请求，断言正常返回 200 OK 且包含 schema 数据
        mockMvc.perform(get("/portal/api/page-builder/pages").param("routePath", "/hidden-landing"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.pageKey").value("hidden_landing_page"))
                .andExpect(jsonPath("$.data.name").value("隐藏营销落地页"));
    }
}
