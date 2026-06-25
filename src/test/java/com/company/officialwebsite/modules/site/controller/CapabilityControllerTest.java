package com.company.officialwebsite.modules.site.controller;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.site.entity.CapabilityCategoryEntity;
import com.company.officialwebsite.modules.site.entity.CapabilityItemEntity;
import com.company.officialwebsite.modules.site.mapper.CapabilityCategoryMapper;
import com.company.officialwebsite.modules.site.mapper.CapabilityItemMapper;
import com.company.officialwebsite.modules.system.mapper.SysAuditLogMapper;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
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

import java.util.List;

/**
 * CapabilityControllerTest：验证核心能力底座分类与子项管理的后台维护与前台暴露的集成测试。
 */
@SpringBootTest
@AutoConfigureMockMvc
class CapabilityControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CapabilityCategoryMapper capabilityCategoryMapper;

    @Autowired
    private CapabilityItemMapper capabilityItemMapper;

    @Autowired
    private SysAuditLogMapper sysAuditLogMapper;

    private static final String CACHE_KEY_SEGMENT = "capabilities";

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cms_capability_item");
        jdbcTemplate.update("DELETE FROM cms_capability_category");
        jdbcTemplate.update("DELETE FROM sys_audit_log");
        redisTemplate.delete(portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT));
    }

    // ==========================================
    // 权限与安全测试
    // ==========================================

    @Test
    void writeOperations_shouldReject_whenNotAuthenticated() throws Exception {
        // 未登录调用分类新增
        mockMvc.perform(post("/admin/api/site/capability-categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"企业经营\",\"visible\":true}"))
                .andExpect(status().isUnauthorized());
    }

    // ==========================================
    // 分类管理 (Category CRUD)
    // ==========================================

    @Test
    void categoryCRUD_shouldSuccess_andRecordAudit() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 1. 新增分类
        String createJson = """
                {
                  "name": "企业经营管理能力",
                  "visible": true,
                  "sortOrder": 10
                }
                """;
        String responseContent = mockMvc.perform(post("/admin/api/site/capability-categories")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn().getResponse().getContentAsString();

        Long categoryId = objectMapper.readTree(responseContent).path("data").asLong();
        Assertions.assertTrue(categoryId > 0);

        // 验证分类数据库插入
        CapabilityCategoryEntity categoryEntity = capabilityCategoryMapper.selectById(categoryId);
        Assertions.assertNotNull(categoryEntity);
        Assertions.assertEquals("企业经营管理能力", categoryEntity.getName());
        Assertions.assertTrue(categoryEntity.getVisible());
        Assertions.assertEquals(10, categoryEntity.getSortOrder());
        Assertions.assertEquals(0, categoryEntity.getVersion());

        // 验证审计日志记录
        Long auditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_audit_log WHERE module_name = 'CAPABILITY' AND action_name = 'CREATE' AND target_type = 'CATEGORY' AND target_id = ?",
                Long.class, categoryId);
        Assertions.assertEquals(1L, auditCount);

        // 2. 修改分类
        String updateJson = """
                {
                  "version": 0,
                  "name": "企业经营管理能力(新版)",
                  "visible": false,
                  "sortOrder": 5
                }
                """;
        mockMvc.perform(put("/admin/api/site/capability-categories/" + categoryId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        CapabilityCategoryEntity updatedCategory = capabilityCategoryMapper.selectById(categoryId);
        Assertions.assertEquals("企业经营管理能力(新版)", updatedCategory.getName());
        Assertions.assertFalse(updatedCategory.getVisible());
        Assertions.assertEquals(5, updatedCategory.getSortOrder());
        Assertions.assertEquals(1, updatedCategory.getVersion());

        // 3. 逻辑删除分类
        mockMvc.perform(delete("/admin/api/site/capability-categories/" + categoryId)
                        .session(session)
                        .with(csrf())
                        .param("version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        CapabilityCategoryEntity deletedCategory = capabilityCategoryMapper.selectById(categoryId);
        Assertions.assertNull(deletedCategory); // logic delete filters it out in selectById
    }

    @Test
    void createCategory_shouldFail_whenNameDuplicate() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 插入初始化分类
        CapabilityCategoryEntity existing = new CapabilityCategoryEntity();
        existing.setName("全域数据智能");
        existing.setVisible(true);
        existing.setSortOrder(2);
        capabilityCategoryMapper.insert(existing);

        // 尝试插入同名分类
        mockMvc.perform(post("/admin/api/site/capability-categories")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" 全域数据智能 \", \"visible\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30802)); // SITE_CAPABILITY_CATEGORY_NAME_DUPLICATE
    }

    @Test
    void updateCategory_shouldFail_whenOptimisticLockConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();

        CapabilityCategoryEntity existing = new CapabilityCategoryEntity();
        existing.setName("全域数据智能");
        existing.setVisible(true);
        existing.setSortOrder(2);
        capabilityCategoryMapper.insert(existing);

        // 使用错误的 version (比如 99) 更新
        mockMvc.perform(put("/admin/api/site/capability-categories/" + existing.getId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\": 99, \"name\":\"全域数据智能(更新)\", \"visible\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    // ==========================================
    // 子项管理 (Capability Item CRUD)
    // ==========================================

    @Test
    void itemCRUD_shouldSuccess_andRecordAudit() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 插入有效分类
        CapabilityCategoryEntity category = new CapabilityCategoryEntity();
        category.setName("企业经营");
        category.setVisible(true);
        category.setSortOrder(1);
        capabilityCategoryMapper.insert(category);

        // 1. 新增子项
        String createJson = String.format("""
                {
                  "categoryId": %d,
                  "name": "ERP 集团管控",
                  "visible": true,
                  "sortOrder": 10
                }
                """, category.getId());

        String responseContent = mockMvc.perform(post("/admin/api/site/capability-items")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andReturn().getResponse().getContentAsString();

        Long itemId = objectMapper.readTree(responseContent).path("data").asLong();
        Assertions.assertTrue(itemId > 0);

        CapabilityItemEntity itemEntity = capabilityItemMapper.selectById(itemId);
        Assertions.assertNotNull(itemEntity);
        Assertions.assertEquals("ERP 集团管控", itemEntity.getName());
        Assertions.assertEquals(category.getId(), itemEntity.getCategoryId());

        // 2. 修改子项
        String updateJson = String.format("""
                {
                  "version": 0,
                  "categoryId": %d,
                  "name": "ERP 集团管控(新版)",
                  "visible": true,
                  "sortOrder": 15
                }
                """, category.getId());

        mockMvc.perform(put("/admin/api/site/capability-items/" + itemId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        CapabilityItemEntity updatedItem = capabilityItemMapper.selectById(itemId);
        Assertions.assertEquals("ERP 集团管控(新版)", updatedItem.getName());
        Assertions.assertEquals(1, updatedItem.getVersion());

        // 3. 删除子项
        mockMvc.perform(delete("/admin/api/site/capability-items/" + itemId)
                        .session(session)
                        .with(csrf())
                        .param("version", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertNull(capabilityItemMapper.selectById(itemId));
    }

    @Test
    void createItem_shouldFail_whenCategoryNotFound() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 传入一个不存在的 categoryId (如 99999L)
        String createJson = """
                {
                  "categoryId": 99999,
                  "name": "ERP 集团管控",
                  "visible": true,
                  "sortOrder": 10
                }
                """;

        mockMvc.perform(post("/admin/api/site/capability-items")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(30801)); // SITE_CAPABILITY_CATEGORY_NOT_FOUND
    }

    @Test
    void createItem_shouldFail_whenNameDuplicateInCategory() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 插入有效分类
        CapabilityCategoryEntity category = new CapabilityCategoryEntity();
        category.setName("企业经营");
        category.setVisible(true);
        category.setSortOrder(1);
        capabilityCategoryMapper.insert(category);

        // 插入第一个子项
        CapabilityItemEntity existingItem = new CapabilityItemEntity();
        existingItem.setCategoryId(category.getId());
        existingItem.setName("业财一体化");
        existingItem.setVisible(true);
        existingItem.setSortOrder(20);
        capabilityItemMapper.insert(existingItem);

        // 尝试插入同分类下的同名子项
        String createJson = String.format("""
                {
                  "categoryId": %d,
                  "name": " 业财一体化 ",
                  "visible": true
                }
                """, category.getId());

        mockMvc.perform(post("/admin/api/site/capability-items")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(30902)); // SITE_CAPABILITY_ITEM_NAME_DUPLICATE
    }

    // ==========================================
    // 级联逻辑删除测试
    // ==========================================

    @Test
    void deleteCategory_shouldCascadeDeleteItems() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 创建分类
        CapabilityCategoryEntity category = new CapabilityCategoryEntity();
        category.setName("企业经营");
        category.setVisible(true);
        category.setSortOrder(1);
        capabilityCategoryMapper.insert(category);

        // 创建属于该分类的 2 个子项
        CapabilityItemEntity item1 = new CapabilityItemEntity();
        item1.setCategoryId(category.getId());
        item1.setName("ERP");
        item1.setVisible(true);
        capabilityItemMapper.insert(item1);

        CapabilityItemEntity item2 = new CapabilityItemEntity();
        item2.setCategoryId(category.getId());
        item2.setName("CRM");
        item2.setVisible(true);
        capabilityItemMapper.insert(item2);

        // 删除分类
        mockMvc.perform(delete("/admin/api/site/capability-categories/" + category.getId())
                        .session(session)
                        .with(csrf())
                        .param("version", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        // 验证分类已被删除
        Assertions.assertNull(capabilityCategoryMapper.selectById(category.getId()));

        // 验证底下的所有子项均已被逻辑删除
        List<CapabilityItemEntity> activeItems = capabilityItemMapper.selectList(
                new LambdaQueryWrapper<CapabilityItemEntity>()
                        .eq(CapabilityItemEntity::getCategoryId, category.getId())
                        .eq(CapabilityItemEntity::getDeletedMarker, 0L));
        Assertions.assertTrue(activeItems.isEmpty());
    }

    // ==========================================
    // 批量排序测试 (Batch Sort)
    // ==========================================

    @Test
    void batchSortCategories_shouldSuccess() throws Exception {
        MockHttpSession session = loginAsAdmin();

        CapabilityCategoryEntity cat1 = new CapabilityCategoryEntity();
        cat1.setName("分类一");
        cat1.setVisible(true);
        cat1.setSortOrder(10);
        capabilityCategoryMapper.insert(cat1);

        CapabilityCategoryEntity cat2 = new CapabilityCategoryEntity();
        cat2.setName("分类二");
        cat2.setVisible(true);
        cat2.setSortOrder(20);
        capabilityCategoryMapper.insert(cat2);

        String sortJson = String.format("""
                [
                  {"id": %d, "sortOrder": 50},
                  {"id": %d, "sortOrder": 40}
                ]
                """, cat1.getId(), cat2.getId());

        mockMvc.perform(put("/admin/api/site/capability-categories/batch-sort")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(sortJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Assertions.assertEquals(50, capabilityCategoryMapper.selectById(cat1.getId()).getSortOrder());
        Assertions.assertEquals(40, capabilityCategoryMapper.selectById(cat2.getId()).getSortOrder());
    }

    // ==========================================
    // 前台 Portal 接口及缓存一致性测试
    // ==========================================

    @Test
    void getPortalCapabilities_shouldEvictCache_uponWriteOperations() throws Exception {
        MockHttpSession session = loginAsAdmin();

        // 1. 创建前台可见数据
        CapabilityCategoryEntity cat = new CapabilityCategoryEntity();
        cat.setName("前台分类");
        cat.setVisible(true);
        cat.setSortOrder(1);
        capabilityCategoryMapper.insert(cat);

        CapabilityItemEntity item = new CapabilityItemEntity();
        item.setCategoryId(cat.getId());
        item.setName("前台子项");
        item.setVisible(true);
        item.setSortOrder(10);
        capabilityItemMapper.insert(item);

        // 2. 调用前台接口并验证数据及缓存被写入
        String cacheKey = portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT);
        Assertions.assertNull(redisTemplate.opsForValue().get(cacheKey));

        mockMvc.perform(get("/portal/api/site/capabilities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data[0].name").value("前台分类"))
                .andExpect(jsonPath("$.data[0].items[0].name").value("前台子项"));

        // 验证 Redis 中已写入缓存
        Assertions.assertNotNull(redisTemplate.opsForValue().get(cacheKey));

        // 3. 后台执行写操作（如修改子项名称），事务提交后将触发缓存失效
        String updateJson = String.format("""
                {
                  "version": 0,
                  "categoryId": %d,
                  "name": "前台子项(新版)",
                  "visible": true,
                  "sortOrder": 10
                }
                """, cat.getId());

        mockMvc.perform(put("/admin/api/site/capability-items/" + item.getId())
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson))
                .andExpect(status().isOk());

        // 验证缓存已被清除
        Assertions.assertNull(redisTemplate.opsForValue().get(cacheKey));
    }
}
