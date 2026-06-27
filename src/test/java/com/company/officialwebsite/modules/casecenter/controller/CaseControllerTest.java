package com.company.officialwebsite.modules.casecenter.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.system.mapper.SysAuditLogMapper;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CaseControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CaseMapper caseMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    private static final String CACHE_KEY_SEGMENT = "cases";

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE id > 0");
        jdbcTemplate.update("DELETE FROM cms_case WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));
    }

    @Test
    void adminGetCases_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/cases"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void portalGetCases_shouldReturnEmptyList_whenNoDataExists() throws Exception {
        mockMvc.perform(get("/portal/api/cases"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void createCase_shouldSuccess_withValidParameters() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        MvcResult result = mockMvc.perform(post("/admin/api/cases")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "天山材料｜商混MVP平台建设",
                                  "logoMediaId": %d,
                                  "summary": "建设集团级商混管理平台，赋能水泥、混凝土一体化管控",
                                  "keywords": ["集团管控", "数据集中", "多区域运营"],
                                  "visible": true
                                }
                                """.formatted(logoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(1, responseNode.get("data").size());

        CaseEntity entity = caseMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CaseEntity>()
                        .eq(CaseEntity::getTitle, "天山材料｜商混MVP平台建设")
                        .eq(CaseEntity::getDeletedMarker, 0L));
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(List.of("集团管控", "数据集中", "多区域运营"), entity.getKeywords());

        MediaAssetEntity asset = mediaAssetMapper.selectById(logoId);
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.name(), asset.getStatus());

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_audit_log WHERE module_name = 'CASE_CENTER' AND action_name = 'CREATE_CASE' AND target_type = 'CASE'",
                Integer.class);
        Assertions.assertEquals(1, auditCount);
    }

    @Test
    void createCase_shouldFail_whenTitleDuplicate() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        CaseEntity entity = new CaseEntity();
        entity.setTitle("天山材料｜商混MVP平台建设");
        entity.setLogoMediaId(logoId);
        entity.setSummary("摘要");
        entity.setKeywords(List.of("标签"));
        entity.setVisible(true);
        entity.setSortOrder(10);
        caseMapper.insert(entity);

        mockMvc.perform(post("/admin/api/cases")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " 天山材料｜商混MVP平台建设 ",
                                  "logoMediaId": %d,
                                  "summary": "建设集团级商混管理平台",
                                  "keywords": ["标签"],
                                  "visible": true
                                }
                                """.formatted(logoId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.CASE_TITLE_DUPLICATE));
    }

    @Test
    void createCase_shouldFail_whenLogoInvalid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(post("/admin/api/cases")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "错图标案例",
                                  "logoMediaId": 999999,
                                  "summary": "摘要",
                                  "visible": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.CASE_LOGO_INVALID));
    }

    @Test
    void updateCase_shouldFail_whenVersionConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        CaseEntity entity = new CaseEntity();
        entity.setTitle("版本冲突案例");
        entity.setLogoMediaId(logoId);
        entity.setSummary("摘要");
        entity.setKeywords(List.of("标签"));
        entity.setVisible(true);
        entity.setSortOrder(10);
        caseMapper.insert(entity);

        mockMvc.perform(put("/admin/api/cases/" + entity.getId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 99,
                                  "title": "版本冲突案例(更新)",
                                  "logoMediaId": %d,
                                  "summary": "摘要更新",
                                  "keywords": ["标签"],
                                  "visible": true
                                }
                                """.formatted(logoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void deleteCase_shouldSuccess_andUnbindMedia() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        CaseEntity entity = new CaseEntity();
        entity.setTitle("待删除案例");
        entity.setLogoMediaId(logoId);
        entity.setSummary("摘要");
        entity.setKeywords(List.of("标签"));
        entity.setVisible(true);
        entity.setSortOrder(10);
        caseMapper.insert(entity);
        mediaAssetService.bindMedia(logoId, "CASE_CENTER", entity.getId(), "logo");

        mockMvc.perform(delete("/admin/api/cases/" + entity.getId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertNull(caseMapper.selectById(entity.getId()));
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.name(), mediaAssetMapper.selectById(logoId).getStatus());
    }

    @Test
    void batchSortCases_shouldSuccess_withFullCoverage() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        CaseEntity first = createCase("案例A", logoId, 10);
        CaseEntity second = createCase("案例B", logoId, 20);

        mockMvc.perform(post("/admin/api/cases/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderedIds": [%d, %d]
                                }
                                """.formatted(second.getId(), first.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertEquals(10, caseMapper.selectById(second.getId()).getSortOrder());
        Assertions.assertEquals(20, caseMapper.selectById(first.getId()).getSortOrder());
    }

    @Test
    void batchSortCases_shouldFail_whenOrderedIdsContainDuplicate() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        CaseEntity first = createCase("案例A", logoId, 10);

        mockMvc.perform(post("/admin/api/cases/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderedIds": [%d, %d]
                                }
                                """.formatted(first.getId(), first.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void batchSortCases_shouldFail_whenOrderedIdsDoNotCoverAllActiveCases() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        CaseEntity first = createCase("案例A", logoId, 10);
        createCase("案例B", logoId, 20);

        mockMvc.perform(post("/admin/api/cases/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderedIds": [%d]
                                }
                                """.formatted(first.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    private CaseEntity createCase(String title, Long logoId, int sortOrder) {
        CaseEntity entity = new CaseEntity();
        entity.setTitle(title);
        entity.setLogoMediaId(logoId);
        entity.setSummary("摘要");
        entity.setKeywords(List.of("标签"));
        entity.setVisible(true);
        entity.setSortOrder(sortOrder);
        caseMapper.insert(entity);
        return entity;
    }

    private Long createPublicMediaAsset() {
        MediaAssetEntity asset = new MediaAssetEntity();
        asset.setMediaType("IMAGE");
        asset.setStatus(MediaAssetStatusEnum.TEMPORARY.name());
        asset.setOriginalFilename("logo.png");
        asset.setContentType("image/png");
        asset.setStoragePath("/tmp/logo.png");
        asset.setPublicUrl("/media/logo.png");
        asset.setFileSize(1024L);
        mediaAssetMapper.insert(asset);
        return asset.getId();
    }
}
