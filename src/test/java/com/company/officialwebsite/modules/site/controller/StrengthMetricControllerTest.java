package com.company.officialwebsite.modules.site.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.site.entity.StrengthMetricEntity;
import com.company.officialwebsite.modules.site.mapper.StrengthMetricMapper;
import com.company.officialwebsite.modules.system.mapper.SysAuditLogMapper;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * StrengthMetricControllerTest：验证企业实力核心指标大盘后台维护与前台暴露的核心契约。
 *
 * <p>覆盖范围：
 * <ul>
 *   <li>未登录访问 Admin 接口返回 401</li>
 *   <li>新增（含图标、不含图标、重复 label）</li>
 *   <li>编辑（图标变更媒体绑定、乐观锁版本冲突）</li>
 *   <li>删除（逻辑删除、媒体解绑、Portal 不再暴露）</li>
 *   <li>批量排序（完整集合、不完整集合拒绝）</li>
 *   <li>Portal 仅返回可见指标</li>
 *   <li>写操作触发缓存失效</li>
 *   <li>写操作生成审计日志</li>
 * </ul>
 */
@SpringBootTest
@AutoConfigureMockMvc
class StrengthMetricControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StrengthMetricMapper strengthMetricMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    private static final String CACHE_KEY_SEGMENT = "strength_metrics";

    @BeforeEach
    void setUp() {
        // 清理业务数据，保留种子数据（id < 0）
        jdbcTemplate.update("DELETE FROM media_reference WHERE id > 0");
        jdbcTemplate.update("DELETE FROM cms_strength_metric WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        // 恢复种子指标的可见性和排序，保证每个测试环境一致
        jdbcTemplate.update("""
                UPDATE cms_strength_metric
                SET visible = 1, deleted_marker = 0,
                    sort_order = CASE id
                        WHEN -9401 THEN 10
                        WHEN -9402 THEN 20
                        WHEN -9403 THEN 30
                    END
                WHERE id IN (-9401, -9402, -9403)
                """);
        // 清除 Portal 缓存，避免测试之间缓存污染
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));
    }

    // ─────────────────────────── 权限测试 ───────────────────────────

    @Test
    void adminGetMetrics_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/site/strength-metrics"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void createMetric_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/admin/api/site/strength-metrics")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"metricValue\":\"50+\",\"label\":\"新标签\",\"visible\":true}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    // ─────────────────────────── Portal 只读接口 ───────────────────────────

    @Test
    void portalGetMetrics_shouldReturnSeedMetrics_inSortOrder() throws Exception {
        mockMvc.perform(get("/portal/api/site/strength-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].label").value("服务客户"))
                .andExpect(jsonPath("$.data[1].label").value("行业覆盖"))
                .andExpect(jsonPath("$.data[2].label").value("项目交付"));
    }

    @Test
    void portalGetMetrics_shouldNotExposeAuditFields() throws Exception {
        mockMvc.perform(get("/portal/api/site/strength-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].version").doesNotExist())
                .andExpect(jsonPath("$.data[0].deletedMarker").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist());
    }

    @Test
    void portalGetMetrics_shouldNotReturnHiddenMetric() throws Exception {
        // 隐藏其中一条种子指标
        jdbcTemplate.update("UPDATE cms_strength_metric SET visible = 0 WHERE id = -9401");
        // 清除缓存，确保本次请求走数据库
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));

        mockMvc.perform(get("/portal/api/site/strength-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[*].label").value(not(hasItems("服务客户"))));
    }

    // ─────────────────────────── 新增指标 ───────────────────────────

    @Test
    void createMetric_withIcon_shouldBindMediaAndAudit() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "metric-icon.png");

        mockMvc.perform(post("/admin/api/site/strength-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iconId":%d,"metricValue":"10年","label":"深耕行业","visible":true}
                                """.formatted(mediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[*].label").value(hasItems("深耕行业")));

        // 验证媒体绑定状态变为 BOUND
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(),
                mediaAssetMapper.selectById(mediaId).getStatus());
        // 验证审计日志写入
        Assertions.assertEquals(2L, sysAuditLogMapper.selectCount(new LambdaQueryWrapper<>()));
    }

    @Test
    void createMetric_withoutIcon_shouldSucceed() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/strength-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metricValue":"200+","label":"合作伙伴","visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[*].label").value(hasItems("合作伙伴")));
    }

    @Test
    void createMetric_shouldRejectDuplicateLabel() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // "服务客户" 已是种子数据的 label，重复创建应被拒绝
        mockMvc.perform(post("/admin/api/site/strength-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metricValue":"60+","label":"服务客户","visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_STRENGTH_METRIC_LABEL_DUPLICATE));
    }

    @Test
    void createMetric_shouldRejectInvalidIcon() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/strength-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"iconId":999999,"metricValue":"50+","label":"测试指标","visible":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_STRENGTH_METRIC_ICON_INVALID));
    }

    @Test
    void createMetric_shouldRejectBlankMetricValue() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/site/strength-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metricValue":"","label":"合作伙伴","visible":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    // ─────────────────────────── 编辑指标 ───────────────────────────

    @Test
    void updateMetric_shouldMoveIconBindingAndHideFromPortal() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long oldMediaId = uploadImage(session, "old-icon.png");
        Long newMediaId = uploadImage(session, "new-icon.png");
        Long metricId = createMetric(session, oldMediaId, "300+", "已服务企业", true);
        int version = currentVersion(metricId);

        mockMvc.perform(put("/admin/api/site/strength-metrics/{id}", metricId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":%d,"iconId":%d,"metricValue":"350+","label":"已服务企业","visible":false}
                                """.formatted(version, newMediaId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        // 旧图标应解绑
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(),
                mediaAssetMapper.selectById(oldMediaId).getStatus());
        // 新图标应绑定
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.getCode(),
                mediaAssetMapper.selectById(newMediaId).getStatus());

        // 隐藏后 Portal 不应返回该指标
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));
        mockMvc.perform(get("/portal/api/site/strength-metrics"))
                .andExpect(jsonPath("$.data[*].label").value(not(hasItems("已服务企业"))));
    }

    @Test
    void updateMetric_shouldRejectVersionConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long metricId = createMetric(session, null, "50+", "版本冲突测试", true);

        // 传入错误的版本号（当前应为 0，传入 99）
        mockMvc.perform(put("/admin/api/site/strength-metrics/{id}", metricId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":99,"metricValue":"50+","label":"版本冲突测试","visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void updateMetric_shouldReturnNotFound_whenMetricDeleted() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/strength-metrics/999999")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0,"metricValue":"X","label":"不存在","visible":true}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_STRENGTH_METRIC_NOT_FOUND));
    }

    // ─────────────────────────── 删除指标 ───────────────────────────

    @Test
    void deleteMetric_shouldSoftDeleteAndUnbindIcon() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long mediaId = uploadImage(session, "del-icon.png");
        Long metricId = createMetric(session, mediaId, "100+", "待删除指标", true);
        int version = currentVersion(metricId);

        mockMvc.perform(delete("/admin/api/site/strength-metrics/{id}", metricId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        // 验证逻辑删除（deleted_marker 等于自身 ID）
        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_strength_metric WHERE id = ? AND deleted_marker = 0",
                Integer.class, metricId);
        Assertions.assertEquals(0, activeCount == null ? 0 : activeCount);

        // 验证图标媒体资源已解绑
        Assertions.assertEquals(MediaAssetStatusEnum.UNBOUND.getCode(),
                mediaAssetMapper.selectById(mediaId).getStatus());

        // Portal 不再暴露已删除指标
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));
        mockMvc.perform(get("/portal/api/site/strength-metrics"))
                .andExpect(jsonPath("$.data[*].label").value(not(hasItems("待删除指标"))));
    }

    @Test
    void deleteMetric_shouldReturnNotFound_whenIdNotExist() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(delete("/admin/api/site/strength-metrics/999999")
                        .session(session)
                        .with(csrf())
                        .param("version", "0"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_STRENGTH_METRIC_NOT_FOUND));
    }

    // ─────────────────────────── 批量排序 ───────────────────────────

    @Test
    void batchSort_shouldPersistNewOrderAndReflectInPortal() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 将顺序反转：原顺序 -9401(服务客户), -9402(行业覆盖), -9403(项目交付)
        mockMvc.perform(put("/admin/api/site/strength-metrics/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedMetricIds":[-9403,-9402,-9401]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[0].label").value("项目交付"))
                .andExpect(jsonPath("$.data[2].label").value("服务客户"));

        // Portal 反映新顺序
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));
        mockMvc.perform(get("/portal/api/site/strength-metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].label").value("项目交付"));

        // 验证审计日志写入
        Assertions.assertEquals(1L, sysAuditLogMapper.selectCount(new LambdaQueryWrapper<>()));
    }

    @Test
    void batchSort_shouldRejectIncompleteSet() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 只传 2 个，遗漏了 1 个，应被拒绝
        mockMvc.perform(put("/admin/api/site/strength-metrics/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedMetricIds":[-9401,-9402]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void batchSort_shouldRejectDuplicateIds() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/strength-metrics/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedMetricIds":[-9401,-9401,-9402,-9403]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    // ─────────────────────────── 缓存失效验证 ───────────────────────────

    @Test
    void createMetric_shouldInvalidatePortalCache() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 先预热缓存
        mockMvc.perform(get("/portal/api/site/strength-metrics")).andExpect(status().isOk());
        String cacheKey = portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT);
        Assertions.assertNotNull(redisTemplate.opsForValue().get(cacheKey), "缓存应已预热");

        // 新增触发写操作
        mockMvc.perform(post("/admin/api/site/strength-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"metricValue":"99%","label":"缓存失效测试","visible":true}
                                """))
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        // 写操作后缓存应失效
        Assertions.assertNull(redisTemplate.opsForValue().get(cacheKey), "写操作后缓存应被清除");
    }

    // ─────────────────────────── 辅助方法 ───────────────────────────

    private Long uploadImage(MockHttpSession session, String filename) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", filename, "image/png", TestConstants.PNG_BYTES);
        MvcResult result = mockMvc.perform(multipart("/admin/api/media/assets")
                        .file(file)
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("mediaId").asLong();
    }

    /**
     * 调用 Admin 接口新增一条指标，返回新增记录的 ID。
     * iconId 传 null 时不携带图标字段。
     */
    private Long createMetric(MockHttpSession session, Long iconId, String metricValue,
                               String label, boolean visible) throws Exception {
        String iconPart = iconId != null ? "\"iconId\":%d,".formatted(iconId) : "";
        mockMvc.perform(post("/admin/api/site/strength-metrics")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {%s"metricValue":"%s","label":"%s","visible":%s}
                                """.formatted(iconPart, metricValue, label, visible)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cms_strength_metric WHERE label = ? AND deleted_marker = 0",
                Long.class, label);
    }

    private int currentVersion(Long metricId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_strength_metric WHERE id = ?", Integer.class, metricId);
        return version == null ? 0 : version;
    }
}
