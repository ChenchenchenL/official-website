package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.site.converter.CapabilityCategoryConverter;
import com.company.officialwebsite.modules.site.converter.CapabilityItemConverter;
import com.company.officialwebsite.modules.site.dto.CapabilityCategoryCreateDTO;
import com.company.officialwebsite.modules.site.dto.CapabilityCategorySortItemDTO;
import com.company.officialwebsite.modules.site.dto.CapabilityCategoryUpdateDTO;
import com.company.officialwebsite.modules.site.entity.CapabilityCategoryEntity;
import com.company.officialwebsite.modules.site.entity.CapabilityItemEntity;
import com.company.officialwebsite.modules.site.mapper.CapabilityCategoryMapper;
import com.company.officialwebsite.modules.site.mapper.CapabilityItemMapper;
import com.company.officialwebsite.modules.site.service.CapabilityCategoryService;
import com.company.officialwebsite.modules.site.service.CapabilityItemService;
import com.company.officialwebsite.modules.site.vo.CapabilityCategoryVO;
import com.company.officialwebsite.modules.site.vo.CapabilityItemVO;
import com.company.officialwebsite.modules.site.vo.PortalCapabilityCategoryVO;
import com.company.officialwebsite.modules.site.vo.PortalCapabilityItemVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CapabilityCategoryServiceImpl：核心能力底座分类业务接口实现。
 */
@Service
public class CapabilityCategoryServiceImpl implements CapabilityCategoryService {

    private static final Logger log = LoggerFactory.getLogger(CapabilityCategoryServiceImpl.class);

    private static final String BIZ_MODULE = "CAPABILITY";
    private static final String TARGET_TYPE = "CATEGORY";
    private static final String CACHE_SEGMENT = "capabilities";

    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_DELETE = "DELETE";
    private static final String ACTION_REORDER = "BATCH_SORT";

    private final CapabilityCategoryMapper capabilityCategoryMapper;
    private final CapabilityItemMapper capabilityItemMapper;
    private final CapabilityCategoryConverter capabilityCategoryConverter;
    private final CapabilityItemConverter capabilityItemConverter;
    private final CapabilityItemService capabilityItemService;
    private final AuditLogService auditLogService;
    private final PortalCacheInvalidationSupport portalCacheInvalidationSupport;
    private final PortalCacheKeyBuilder portalCacheKeyBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final OfficialProperties officialProperties;

    public CapabilityCategoryServiceImpl(
            CapabilityCategoryMapper capabilityCategoryMapper,
            CapabilityItemMapper capabilityItemMapper,
            CapabilityCategoryConverter capabilityCategoryConverter,
            CapabilityItemConverter capabilityItemConverter,
            @Lazy CapabilityItemService capabilityItemService,
            AuditLogService auditLogService,
            PortalCacheInvalidationSupport portalCacheInvalidationSupport,
            PortalCacheKeyBuilder portalCacheKeyBuilder,
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper,
            OfficialProperties officialProperties) {
        this.capabilityCategoryMapper = capabilityCategoryMapper;
        this.capabilityItemMapper = capabilityItemMapper;
        this.capabilityCategoryConverter = capabilityCategoryConverter;
        this.capabilityItemConverter = capabilityItemConverter;
        this.capabilityItemService = capabilityItemService;
        this.auditLogService = auditLogService;
        this.portalCacheInvalidationSupport = portalCacheInvalidationSupport;
        this.portalCacheKeyBuilder = portalCacheKeyBuilder;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CapabilityCategoryVO> getAdminCategoryTree() {
        // 1. 查询所有未逻辑删除的分类
        List<CapabilityCategoryEntity> categories = capabilityCategoryMapper.selectList(
                new LambdaQueryWrapper<CapabilityCategoryEntity>()
                        .eq(CapabilityCategoryEntity::getDeletedMarker, 0L)
                        .orderByAsc(CapabilityCategoryEntity::getSortOrder)
                        .orderByAsc(CapabilityCategoryEntity::getId));

        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询所有未逻辑删除的子项
        List<CapabilityItemEntity> items = capabilityItemMapper.selectList(
                new LambdaQueryWrapper<CapabilityItemEntity>()
                        .eq(CapabilityItemEntity::getDeletedMarker, 0L)
                        .orderByAsc(CapabilityItemEntity::getSortOrder)
                        .orderByAsc(CapabilityItemEntity::getId));

        // 3. 将子项按 categoryId 分组并转换成 VO
        Map<Long, List<CapabilityItemVO>> itemsGrouped = items.stream()
                .map(capabilityItemConverter::toAdminVO)
                .collect(Collectors.groupingBy(CapabilityItemVO::getCategoryId));

        // 4. 组装树状结构
        return categories.stream()
                .map(cat -> capabilityCategoryConverter.toAdminVO(cat, itemsGrouped.getOrDefault(cat.getId(), Collections.emptyList())))
                .toList();
    }

    @Override
    @Transactional
    public Long createCategory(CapabilityCategoryCreateDTO dto) {
        String name = dto.getName().trim();
        validateNameUnique(null, name);

        CapabilityCategoryEntity entity = new CapabilityCategoryEntity();
        entity.setName(name);
        entity.setVisible(dto.getVisible());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 99);

        try {
            capabilityCategoryMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_CATEGORY_NAME_DUPLICATE);
        }

        log.info("create capability category success categoryId={} name={}", entity.getId(), entity.getName());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateCategory(Long id, CapabilityCategoryUpdateDTO dto) {
        CapabilityCategoryEntity entity = requireActiveCategory(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), dto.getVersion());

        Map<String, Object> before = toSnapshot(entity);
        String newName = dto.getName().trim();
        validateNameUnique(id, newName);

        entity.setName(newName);
        entity.setVisible(dto.getVisible());
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }

        try {
            ConcurrencyHelper.tryUpdate(capabilityCategoryMapper, entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_CATEGORY_NAME_DUPLICATE);
        }

        log.info("update capability category success categoryId={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void deleteCategory(Long id, Integer version) {
        CapabilityCategoryEntity entity = requireActiveCategory(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);

        Map<String, Object> before = toSnapshot(entity);

        // 1. 逻辑删除分类
        int deleted = capabilityCategoryMapper.delete(
                new LambdaUpdateWrapper<CapabilityCategoryEntity>()
                        .eq(CapabilityCategoryEntity::getId, entity.getId())
                        .eq(CapabilityCategoryEntity::getVersion, version));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }

        // 2. 级联逻辑删除底下的所有子项
        capabilityItemService.deleteItemsByCategoryId(entity.getId());

        log.info("delete capability category success categoryId={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void batchSortCategories(List<CapabilityCategorySortItemDTO> requestDTO) {
        if (requestDTO == null || requestDTO.isEmpty()) {
            log.warn("batch sort categories received empty list");
            return;
        }

        List<Long> requestedIds = requestDTO.stream().map(CapabilityCategorySortItemDTO::getId).toList();
        Set<Long> deduplicatedIds = new LinkedHashSet<>(requestedIds);
        if (deduplicatedIds.size() != requestedIds.size()) {
            log.warn("batch sort categories duplicate ids detected requestedIds={}", requestedIds);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复的分类项");
        }

        List<CapabilityCategoryEntity> activeCategories = capabilityCategoryMapper.selectList(
                new LambdaQueryWrapper<CapabilityCategoryEntity>()
                        .eq(CapabilityCategoryEntity::getDeletedMarker, 0L)
                        .orderByAsc(CapabilityCategoryEntity::getId));
        if (activeCategories.isEmpty()) {
            log.warn("no active categories to sort");
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_CATEGORY_NOT_FOUND, "暂无可排序的分类");
        }

        Set<Long> currentIds = new LinkedHashSet<>(activeCategories.stream().map(CapabilityCategoryEntity::getId).toList());
        if (!deduplicatedIds.equals(currentIds)) {
            log.warn("batch sort categories completeness check failed requestedIds={} currentIds={}", deduplicatedIds, currentIds);
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前全部活跃底座分类");
        }

        Map<Long, CapabilityCategoryEntity> entityMap = new HashMap<>();
        for (CapabilityCategoryEntity cat : activeCategories) {
            entityMap.put(cat.getId(), cat);
        }

        List<Map<String, Object>> before = activeCategories.stream()
                .sorted(Comparator.comparing(CapabilityCategoryEntity::getSortOrder).thenComparing(CapabilityCategoryEntity::getId))
                .map(this::toSnapshot)
                .toList();

        for (CapabilityCategorySortItemDTO item : requestDTO) {
            CapabilityCategoryEntity entity = entityMap.get(item.getId());
            if (entity == null) {
                log.error("batch sort category not found in map categoryId={}", item.getId());
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "分类已被删除，请刷新后重试");
            }
            entity.setSortOrder(item.getSortOrder());
            ConcurrencyHelper.tryUpdate(capabilityCategoryMapper, entity);
        }

        List<Map<String, Object>> after = requestDTO.stream()
                .map(item -> entityMap.get(item.getId()))
                .map(this::toSnapshot)
                .toList();

        log.info("batch sort categories success count={}", requestDTO.size());
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalCapabilityCategoryVO> getPortalCapabilities() {
        String cacheKey = portalCacheKeyBuilder.build(CACHE_SEGMENT);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.convertValue(
                        cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, PortalCapabilityCategoryVO.class));
            }
        } catch (Exception ex) {
            log.warn("read portal capabilities cache failed key={}", cacheKey, ex);
        }

        // 1. 查询所有未逻辑删除且可见的分类
        List<CapabilityCategoryEntity> categories = capabilityCategoryMapper.selectList(
                new LambdaQueryWrapper<CapabilityCategoryEntity>()
                        .eq(CapabilityCategoryEntity::getDeletedMarker, 0L)
                        .eq(CapabilityCategoryEntity::getVisible, true)
                        .orderByAsc(CapabilityCategoryEntity::getSortOrder)
                        .orderByAsc(CapabilityCategoryEntity::getId));

        if (categories.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 查询所有未逻辑删除且可见的子项
        List<CapabilityItemEntity> items = capabilityItemMapper.selectList(
                new LambdaQueryWrapper<CapabilityItemEntity>()
                        .eq(CapabilityItemEntity::getDeletedMarker, 0L)
                        .eq(CapabilityItemEntity::getVisible, true)
                        .orderByAsc(CapabilityItemEntity::getSortOrder)
                        .orderByAsc(CapabilityItemEntity::getId));

        // 3. 过滤出归属于可见分类的子项，并分组
        Set<Long> visibleCategoryIds = categories.stream().map(CapabilityCategoryEntity::getId).collect(Collectors.toSet());
        Map<Long, List<PortalCapabilityItemVO>> groupedPortalItems = items.stream()
                .filter(item -> visibleCategoryIds.contains(item.getCategoryId()))
                .collect(Collectors.groupingBy(
                        CapabilityItemEntity::getCategoryId,
                        Collectors.mapping(capabilityItemConverter::toPortalVO, Collectors.toList())
                ));

        // 4. 组装 VO 列表
        List<PortalCapabilityCategoryVO> result = categories.stream()
                .map(cat -> capabilityCategoryConverter.toPortalVO(cat, groupedPortalItems.getOrDefault(cat.getId(), Collections.emptyList())))
                .toList();

        try {
            Duration ttl = officialProperties.getCache().getDefaultTtl();
            redisTemplate.opsForValue().set(cacheKey, result, ttl);
        } catch (Exception ex) {
            log.warn("write portal capabilities cache failed key={}", cacheKey, ex);
        }
        return result;
    }

    @Override
    public boolean categoryExists(Long id) {
        if (id == null) {
            return false;
        }
        Long count = capabilityCategoryMapper.selectCount(
                new LambdaQueryWrapper<CapabilityCategoryEntity>()
                        .eq(CapabilityCategoryEntity::getId, id)
                        .eq(CapabilityCategoryEntity::getDeletedMarker, 0L));
        return count != null && count > 0;
    }

    private CapabilityCategoryEntity requireActiveCategory(Long id) {
        CapabilityCategoryEntity entity = capabilityCategoryMapper.selectOne(
                new LambdaQueryWrapper<CapabilityCategoryEntity>()
                        .eq(CapabilityCategoryEntity::getId, id)
                        .eq(CapabilityCategoryEntity::getDeletedMarker, 0L));
        if (entity == null) {
            log.warn("capability category not found categoryId={}", id);
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_CATEGORY_NOT_FOUND);
        }
        return entity;
    }

    private void validateNameUnique(Long excludeId, String name) {
        LambdaQueryWrapper<CapabilityCategoryEntity> queryWrapper = new LambdaQueryWrapper<CapabilityCategoryEntity>()
                .eq(CapabilityCategoryEntity::getName, name)
                .eq(CapabilityCategoryEntity::getDeletedMarker, 0L);
        if (excludeId != null) {
            queryWrapper.ne(CapabilityCategoryEntity::getId, excludeId);
        }
        Long count = capabilityCategoryMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            log.warn("capability category name duplicate name={}", name);
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_CATEGORY_NAME_DUPLICATE);
        }
    }

    private void invalidatePortalCache() {
        portalCacheInvalidationSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }

    private Map<String, Object> toSnapshot(CapabilityCategoryEntity entity) {
        if (entity == null) {
            return null;
        }
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("name", entity.getName());
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }
}
