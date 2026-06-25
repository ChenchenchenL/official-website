package com.company.officialwebsite.modules.site.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.site.entity.AiCardEntity;
import com.company.officialwebsite.modules.site.mapper.AiCardMapper;
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
import org.springframework.test.web.servlet.MvcResult;

/**
 * AiCardControllerTest：验证 AI 战略卡片管理的后台维护与前台暴露的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AiCardControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AiCardMapper aiCardMapper;

    @Autowired
    private MediaAssetMapper mediaAssetMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    private static final String CACHE_KEY_SEGMENT = "ai_cards";

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM media_reference WHERE id > 0");
        jdbcTemplate.update("DELETE FROM cms_ai_card WHERE id > 0");
        jdbcTemplate.update("DELETE FROM media_asset WHERE id > 0");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        jdbcTemplate.update("""
                UPDATE cms_ai_card
                SET visible = 1, deleted_marker = 0, version = 0,
                    name = CASE id
                        WHEN -9501 THEN '企业知识库'
                        WHEN -9502 THEN '智能助手'
                        WHEN -9503 THEN '数据智能'
                        WHEN -9504 THEN '企业智能体'
                    END,
                    english_name = CASE id
                        WHEN -9501 THEN 'Knowledge'
                        WHEN -9502 THEN 'Assistant'
                        WHEN -9503 THEN 'Analytics'
                        WHEN -9504 THEN 'Agent'
                    END,
                    description = CASE id
                        WHEN -9501 THEN '沉淀组织经验与业务知识，构建企业专属知识大脑'
                        WHEN -9502 THEN '打造多场景AI助手，实现办公、协同与业务处理效率翻倍'
                        WHEN -9503 THEN '整合全域数据资产，实现智能预测、分析与智能辅助决策'
                        WHEN -9504 THEN '发布自主进化智能体产品，开启自主化业务流运转新阶段'
                    END,
                    sort_order = CASE id
                        WHEN -9501 THEN 1
                        WHEN -9502 THEN 2
                        WHEN -9503 THEN 3
                        WHEN -9504 THEN 4
                    END
                WHERE id IN (-9501, -9502, -9503, -9504)
                """);
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));
    }

    // ─────────────────────────── 权限与基本测试 ───────────────────────────

    @Test
    void adminGetCards_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/site/ai-cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void createCard_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(post("/admin/api/site/ai-cards")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新AI卡片\",\"description\":\"描述\",\"visible\":true}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void portalGetCards_shouldReturnSeedCards_inSortOrder() throws Exception {
        mockMvc.perform(get("/portal/api/site/ai-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].name").value("企业知识库"))
                .andExpect(jsonPath("$.data[1].name").value("智能助手"))
                .andExpect(jsonPath("$.data[2].name").value("数据智能"))
                .andExpect(jsonPath("$.data[3].name").value("企业智能体"));
    }

    @Test
    void portalGetCards_shouldNotExposeAuditFields() throws Exception {
        mockMvc.perform(get("/portal/api/site/ai-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].version").doesNotExist())
                .andExpect(jsonPath("$.data[0].deletedMarker").doesNotExist())
                .andExpect(jsonPath("$.data[0].createdBy").doesNotExist());
    }

    @Test
    void portalGetCards_shouldNotReturnHiddenCard() throws Exception {
        jdbcTemplate.update("UPDATE cms_ai_card SET visible = 0 WHERE id = -9501");
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));

        mockMvc.perform(get("/portal/api/site/ai-cards"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[0].name").value("智能助手"));
    }

    // ─────────────────────────── 后台 CRUD 测试 ───────────────────────────

    @Test
    void createCard_shouldSuccess_withValidParameters() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long iconId = createPublicMediaAsset();

        MvcResult result = mockMvc.perform(post("/admin/api/site/ai-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "多模态大模型",
                                  "englishName": "Multimodal LLM",
                                  "iconId": %d,
                                  "description": "融合文本、图像及语音的全新智能模型服务",
                                  "jumpLink": "/solutions/multimodal",
                                  "visible": true
                                }
                                """.formatted(iconId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn();

        JsonNode responseNode = objectMapper.readTree(result.getResponse().getContentAsString());
        long newId = responseNode.get("data").asLong();
        Assertions.assertTrue(newId > 0);

        // 验证数据库记录
        AiCardEntity entity = aiCardMapper.selectById(newId);
        Assertions.assertNotNull(entity);
        Assertions.assertEquals("多模态大模型", entity.getName());
        Assertions.assertEquals(iconId, entity.getIconId());

        // 验证媒体绑定状态
        MediaAssetEntity asset = mediaAssetMapper.selectById(iconId);
        Assertions.assertEquals(MediaAssetStatusEnum.BOUND.name(), asset.getStatus());

        // 验证审计日志
        int auditCount = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM sys_audit_log WHERE target_id = ?", Integer.class, newId);
        Assertions.assertEquals(1, auditCount);

        // 验证前台缓存已被清空
        Assertions.assertNull(redisTemplate.opsForValue().get(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT)));
    }

    @Test
    void createCard_shouldFail_withDuplicateName() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(post("/admin/api/site/ai-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "企业知识库",
                                  "description": "重复名冲突测试",
                                  "visible": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_AI_CARD_NAME_DUPLICATE));
    }

    @Test
    void createCard_shouldFail_withInvalidIconId() throws Exception {
        MockHttpSession session = loginAsAdmin();
        mockMvc.perform(post("/admin/api/site/ai-cards")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "错误Icon指标",
                                  "iconId": 999999,
                                  "description": "媒体校验失败测试",
                                  "visible": true
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_AI_CARD_ICON_INVALID));
    }

    @Test
    void updateCard_shouldSuccess_whenChangingFields() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long newIconId = createPublicMediaAsset();

        AiCardEntity entity = aiCardMapper.selectById(-9501L);
        Assertions.assertEquals(0, entity.getVersion());

        mockMvc.perform(put("/admin/api/site/ai-cards/-9501")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 0,
                                  "name": "企业知识库(新版)",
                                  "englishName": "Smart Knowledge",
                                  "iconId": %d,
                                  "description": "沉淀组织经验与业务知识，支持RAG智能检索引擎",
                                  "jumpLink": "/solutions/smart-knowledge",
                                  "visible": false
                                }
                                """.formatted(newIconId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        AiCardEntity updated = aiCardMapper.selectById(-9501L);
        Assertions.assertEquals(1, updated.getVersion());
        Assertions.assertEquals("企业知识库(新版)", updated.getName());
        Assertions.assertEquals(newIconId, updated.getIconId());
    }

    @Test
    void updateCard_shouldFail_whenOptimisticLockConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/ai-cards/-9501")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version": 99,
                                  "name": "冲突版本名称",
                                  "description": "并发冲突测试",
                                  "visible": true
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void deleteCard_shouldSuccess_andRemovePortalCache() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(get("/portal/api/site/ai-cards")).andExpect(status().isOk());
        Assertions.assertNotNull(redisTemplate.opsForValue().get(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT)));

        mockMvc.perform(delete("/admin/api/site/ai-cards/-9501")
                        .session(session)
                        .with(csrf())
                        .param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        AiCardEntity deleted = aiCardMapper.selectById(-9501L);
        Assertions.assertNull(deleted);

        Long deletedMarker = jdbcTemplate.queryForObject("SELECT deleted_marker FROM cms_ai_card WHERE id = -9501", Long.class);
        Assertions.assertEquals(Long.valueOf(-9501L), deletedMarker);

        Assertions.assertNull(redisTemplate.opsForValue().get(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT)));
    }

    @Test
    void batchSortCards_shouldSuccess_withValidOrder() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/ai-cards/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "id": -9501, "sortOrder": 10 },
                                  { "id": -9502, "sortOrder": 20 },
                                  { "id": -9503, "sortOrder": 30 },
                                  { "id": -9504, "sortOrder": 40 }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertEquals(10, aiCardMapper.selectById(-9501L).getSortOrder());
        Assertions.assertEquals(20, aiCardMapper.selectById(-9502L).getSortOrder());
        Assertions.assertEquals(30, aiCardMapper.selectById(-9503L).getSortOrder());
        Assertions.assertEquals(40, aiCardMapper.selectById(-9504L).getSortOrder());
    }

    @Test
    void batchSortCards_shouldFail_whenIncompleteList() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(put("/admin/api/site/ai-cards/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  { "id": -9501, "sortOrder": 10 }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    private Long createPublicMediaAsset() {
        MediaAssetEntity asset = new MediaAssetEntity();
        asset.setMediaType("IMAGE");
        asset.setStatus(MediaAssetStatusEnum.TEMPORARY.name());
        asset.setOriginalFilename("ai-knowledge.png");
        asset.setContentType("image/png");
        asset.setStoragePath("/tmp/ai-knowledge.png");
        asset.setPublicUrl("/media/ai-knowledge.png");
        asset.setFileSize(2048L);
        mediaAssetMapper.insert(asset);
        return asset.getId();
    }
}
