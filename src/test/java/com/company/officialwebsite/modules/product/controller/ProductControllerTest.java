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
import com.company.officialwebsite.modules.product.entity.ProductEntity;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * ProductControllerTest：验证产品配置接口的后台管理和前台公开查询功能。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private MediaAssetService mediaAssetService;

    private static final String CACHE_KEY_SEGMENT = "products";

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE id > 0");
        jdbcTemplate.update("DELETE FROM cms_product WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));
    }

    // ─────────────────────────── 鉴权与匿名访问测试 ───────────────────────────

    @Test
    void adminGetProducts_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/products"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void createProduct_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/admin/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新产品\",\"logoId\":1,\"abstractText\":\"产品摘要\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void portalGetProducts_shouldReturnEmptyList_whenNoProductsExist() throws Exception {
        mockMvc.perform(get("/portal/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    // ─────────────────────────── 后台 CRUD 核心测试 ───────────────────────────

    @Test
    void createProduct_shouldSuccess_withValidParameters() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        MvcResult result = mockMvc.perform(post("/admin/api/products")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "云台智管 ERP",
                                  "logoId": %d,
                                  "subTitle": "集团经营管理平台",
                                  "abstractText": "全方位集成ERP解决方案，助力集团业财一体与敏捷运营",
                                  "statusTag": "已发布",
                                  "detailLink": "/product/erp",
                                  "visible": 1,
                                  "sortOrder": 1
                                }
                                """.formatted(logoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        long newId = responseNode.get("data").asLong();
        Assertions.assertTrue(newId > 0);

        // 验证数据库记录
        ProductEntity entity = productMapper.selectById(newId);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("云台智管 ERP", entity.getName());
        Assertions.assertEquals(logoId, entity.getLogoId());
        Assertions.assertEquals(1, entity.getVisible());

        // 验证媒体绑定状态
        MediaAssetEntity asset = mediaAssetMapper.selectById(logoId);
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.name(), asset.getStatus());

        // 验证审计日志
        int auditCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM sys_audit_log WHERE target_id = ?", Integer.class, newId);
        Assertions.assertEquals(1, auditCount);

        // 验证前台缓存被清空
        Assertions.assertNull(redisTemplate.opsForValue().get(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT)));
    }

    @Test
    void createProduct_shouldFail_withDuplicateName() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        // 插入首个产品
        ProductEntity p = new ProductEntity();
        p.setName("云台智瓴 Agent");
        p.setLogoId(logoId);
        p.setAbstractText("摘要");
        p.setVisible(1);
        p.setSortOrder(99);
        productMapper.insert(p);

        // 试图以相同名称新增
        mockMvc.perform(post("/admin/api/products")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "云台智瓴 Agent",
                                  "logoId": %d,
                                  "abstractText": "企业级智能体底座"
                                }
                                """.formatted(logoId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_NAME_DUPLICATE.getCode()));
    }

    @Test
    void createProduct_shouldFail_withInvalidLogoId() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(post("/admin/api/products")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "错Logo产品",
                                  "logoId": 999999,
                                  "abstractText": "产品摘要"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(ErrorCode.PRODUCT_LOGO_INVALID.getCode()));
    }

    @Test
    void createProduct_shouldFail_withInvalidDetailLink() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        mockMvc.perform(post("/admin/api/products")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "错路由产品",
                                  "logoId": %d,
                                  "abstractText": "产品摘要",
                                  "detailLink": "invalid-path-without-slash"
                                }
                                """.formatted(logoId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void updateProduct_shouldSuccess_whenChangingFields() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        ProductEntity entity = new ProductEntity();
        entity.setName("原始产品");
        entity.setLogoId(logoId);
        entity.setAbstractText("原始摘要");
        entity.setVisible(1);
        entity.setSortOrder(50);
        productMapper.insert(entity);
        mediaAssetService.bindMedia(logoId, "PRODUCT", entity.getId(), "logo");

        Long newLogoId = createPublicMediaAsset();

        mockMvc.perform(put("/admin/api/products/" + entity.getId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 0,
                                  "name": "已更新产品",
                                  "logoId": %d,
                                  "abstractText": "更新后的摘要描述",
                                  "detailLink": "/product/updated",
                                  "visible": 0
                                }
                                """.formatted(newLogoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        ProductEntity updated = productMapper.selectById(entity.getId());
        Assertions.assertEquals(1, updated.getVersion());
        Assertions.assertEquals("已更新产品", updated.getName());
        Assertions.assertEquals(newLogoId, updated.getLogoId());
        Assertions.assertEquals(0, updated.getVisible());

        // 原始 Logo 解绑
        MediaAssetEntity oldAsset = mediaAssetMapper.selectById(logoId);
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.name(), oldAsset.getStatus());
        // 新 Logo 绑定
        MediaAssetEntity newAsset = mediaAssetMapper.selectById(newLogoId);
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.name(), newAsset.getStatus());
    }

    @Test
    void updateProduct_shouldFail_whenOptimisticLockConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        ProductEntity entity = new ProductEntity();
        entity.setName("锁测试产品");
        entity.setLogoId(logoId);
        entity.setAbstractText("摘要");
        entity.setVisible(1);
        entity.setSortOrder(50);
        productMapper.insert(entity);

        mockMvc.perform(put("/admin/api/products/" + entity.getId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 99,
                                  "name": "名称冲突",
                                  "logoId": %d,
                                  "abstractText": "摘要",
                                  "visible": 1
                                }
                                """.formatted(logoId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void deleteProduct_shouldSuccess_andRemovePortalCache() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        ProductEntity entity = new ProductEntity();
        entity.setName("待删除产品");
        entity.setLogoId(logoId);
        entity.setAbstractText("摘要");
        entity.setVisible(1);
        entity.setSortOrder(1);
        productMapper.insert(entity);
        mediaAssetService.bindMedia(logoId, "PRODUCT", entity.getId(), "logo");

        // 模拟前台已生成缓存
        mockMvc.perform(get("/portal/api/products")).andExpect(status().isOk());
        Assertions.assertNotNull(redisTemplate.opsForValue().get(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT)));

        mockMvc.perform(delete("/admin/api/products/" + entity.getId())
                        .session(session)
                        .with(csrf())
                        .param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        ProductEntity deleted = productMapper.selectById(entity.getId());
        Assertions.assertNull(deleted);

        // 媒体状态解绑
        MediaAssetEntity asset = mediaAssetMapper.selectById(logoId);
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.name(), asset.getStatus());

        // 前台缓存失效
        Assertions.assertNull(redisTemplate.opsForValue().get(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT)));
    }

    @Test
    void batchSortProducts_shouldSuccess_withValidOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long logoId = createPublicMediaAsset();

        ProductEntity p1 = new ProductEntity();
        p1.setName("产品1");
        p1.setLogoId(logoId);
        p1.setAbstractText("摘要1");
        p1.setVisible(1);
        p1.setSortOrder(1);
        productMapper.insert(p1);

        ProductEntity p2 = new ProductEntity();
        p2.setName("产品2");
        p2.setLogoId(logoId);
        p2.setAbstractText("摘要2");
        p2.setVisible(1);
        p2.setSortOrder(2);
        productMapper.insert(p2);

        mockMvc.perform(put("/admin/api/products/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "id": %d, "sortOrder": 20 },
                                  { "id": %d, "sortOrder": 10 }
                                ]
                                """.formatted(p1.getId(), p2.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertEquals(20, productMapper.selectById(p1.getId()).getSortOrder());
        Assertions.assertEquals(10, productMapper.selectById(p2.getId()).getSortOrder());
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
