package com.company.officialwebsite.modules.product.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionEntity;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionMapper;
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

/**
 * IndustrySolutionControllerTest：验证行业解决方案管理的后台维护和前台公开查询契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class IndustrySolutionControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private IndustrySolutionMapper industrySolutionMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    private static final String CACHE_KEY_SEGMENT = "industry_solutions";

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE id > 0");
        jdbcTemplate.update("DELETE FROM cms_industry_solution WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));
    }

    @Test
    void adminGetIndustrySolutions_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/industry-solutions"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void portalGetIndustrySolutions_shouldReturnEmptyList_whenNoDataExists() throws Exception {
        mockMvc.perform(get("/portal/api/industry-solutions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void createIndustrySolution_shouldSuccess_withValidParameters() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = createPublicMediaAsset();

        MvcResult result = mockMvc.perform(post("/admin/api/industry-solutions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "建材与新材料行业",
                                  "iconMediaId": %d,
                                  "description": "建设集团级商混管理平台，赋能水泥、混凝土一体化管控",
                                  "customerTags": ["天山材料", "南方新材料"],
                                  "visible": true
                                }
                                """.formatted(iconId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        Assertions.assertEquals(1, responseNode.get("data").size());

        IndustrySolutionEntity entity = industrySolutionMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<IndustrySolutionEntity>()
                        .eq(IndustrySolutionEntity::getName, "建材与新材料行业")
                        .eq(IndustrySolutionEntity::getDeletedMarker, 0L));
        Assertions.assertNotNull(entity);
        Assertions.assertEquals(iconId, entity.getIconMediaId());
        Assertions.assertEquals(List.of("天山材料", "南方新材料"), entity.getCustomerTags());

        MediaAssetEntity asset = mediaAssetMapper.selectById(iconId);
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.name(), asset.getStatus());

        Integer auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM sys_audit_log WHERE module_name = 'PRODUCT' AND action_name = 'CREATE_SOLUTION' AND target_type = 'INDUSTRY_SOLUTION'",
                Integer.class);
        Assertions.assertEquals(1, auditCount);
        Assertions.assertNull(redisTemplate.opsForValue().get(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT)));
    }

    @Test
    void createIndustrySolution_shouldFail_whenNameDuplicate() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = createPublicMediaAsset();

        IndustrySolutionEntity entity = new IndustrySolutionEntity();
        entity.setName("建材与新材料行业");
        entity.setIconMediaId(iconId);
        entity.setDescription("摘要");
        entity.setCustomerTags(List.of("天山材料"));
        entity.setVisible(true);
        entity.setSortOrder(10);
        industrySolutionMapper.insert(entity);

        mockMvc.perform(post("/admin/api/industry-solutions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": " 建材与新材料行业 ",
                                  "iconMediaId": %d,
                                  "description": "建设集团级商混管理平台",
                                  "customerTags": ["天山材料"],
                                  "visible": true
                                }
                                """.formatted(iconId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PRODUCT_SOLUTION_NAME_DUPLICATE));
    }

    @Test
    void createIndustrySolution_shouldFail_whenIconInvalid() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(post("/admin/api/industry-solutions")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "错图标行业",
                                  "iconMediaId": 999999,
                                  "description": "摘要",
                                  "visible": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PRODUCT_SOLUTION_ICON_INVALID));
    }

    @Test
    void updateIndustrySolution_shouldFail_whenVersionConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = createPublicMediaAsset();

        IndustrySolutionEntity entity = new IndustrySolutionEntity();
        entity.setName("版本冲突行业");
        entity.setIconMediaId(iconId);
        entity.setDescription("摘要");
        entity.setCustomerTags(List.of("客户A"));
        entity.setVisible(true);
        entity.setSortOrder(10);
        industrySolutionMapper.insert(entity);

        mockMvc.perform(put("/admin/api/industry-solutions/" + entity.getId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 99,
                                  "name": "版本冲突行业(更新)",
                                  "iconMediaId": %d,
                                  "description": "摘要更新",
                                  "customerTags": ["客户A"],
                                  "visible": true
                                }
                                """.formatted(iconId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void deleteIndustrySolution_shouldSuccess_andUnbindMedia() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = createPublicMediaAsset();

        IndustrySolutionEntity entity = new IndustrySolutionEntity();
        entity.setName("待删除行业");
        entity.setIconMediaId(iconId);
        entity.setDescription("摘要");
        entity.setCustomerTags(List.of("客户A"));
        entity.setVisible(true);
        entity.setSortOrder(10);
        industrySolutionMapper.insert(entity);
        mediaAssetService.bindMedia(iconId, "PRODUCT", entity.getId(), "icon");

        mockMvc.perform(delete("/admin/api/industry-solutions/" + entity.getId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertNull(industrySolutionMapper.selectById(entity.getId()));
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.name(), mediaAssetMapper.selectById(iconId).getStatus());
    }

    @Test
    void batchSortIndustrySolutions_shouldSuccess_withFullCoverage() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = createPublicMediaAsset();

        IndustrySolutionEntity first = createSolution("行业A", iconId, 10);
        IndustrySolutionEntity second = createSolution("行业B", iconId, 20);

        mockMvc.perform(post("/admin/api/industry-solutions/reorder")
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

        Assertions.assertEquals(10, industrySolutionMapper.selectById(second.getId()).getSortOrder());
        Assertions.assertEquals(20, industrySolutionMapper.selectById(first.getId()).getSortOrder());
    }

    private IndustrySolutionEntity createSolution(String name, Long iconId, int sortOrder) {
        IndustrySolutionEntity entity = new IndustrySolutionEntity();
        entity.setName(name);
        entity.setIconMediaId(iconId);
        entity.setDescription("摘要");
        entity.setCustomerTags(List.of("客户A"));
        entity.setVisible(true);
        entity.setSortOrder(sortOrder);
        industrySolutionMapper.insert(entity);
        return entity;
    }

    private Long createPublicMediaAsset() {
        MediaAssetEntity asset = new MediaAssetEntity();
        asset.setMediaType("IMAGE");
        asset.setStatus(MediaAssetStatusEnum.TEMPORARY.name());
        asset.setOriginalFilename("icon.png");
        asset.setContentType("image/png");
        asset.setStoragePath("/tmp/icon.png");
        asset.setPublicUrl("/media/icon.png");
        asset.setFileSize(1024L);
        mediaAssetMapper.insert(asset);
        return asset.getId();
    }
}
