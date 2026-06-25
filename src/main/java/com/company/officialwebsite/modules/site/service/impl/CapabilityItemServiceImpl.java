package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.modules.site.dto.CapabilityItemCreateDTO;
import com.company.officialwebsite.modules.site.dto.CapabilityItemSortItemDTO;
import com.company.officialwebsite.modules.site.dto.CapabilityItemUpdateDTO;
import com.company.officialwebsite.modules.site.entity.CapabilityItemEntity;
import com.company.officialwebsite.modules.site.mapper.CapabilityItemMapper;
import com.company.officialwebsite.modules.site.service.CapabilityCategoryService;
import com.company.officialwebsite.modules.site.service.CapabilityItemService;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * CapabilityItemServiceImpl：核心能力具体子项业务接口实现。
 */
@Service
public class CapabilityItemServiceImpl implements CapabilityItemService {

    private static final Logger log = LoggerFactory.getLogger(CapabilityItemServiceImpl.class);

    private static final String BIZ_MODULE = "CAPABILITY";
    private static final String TARGET_TYPE = "ITEM";
    private static final String CACHE_SEGMENT = "capabilities";

    private static final String ACTION_CREATE = "CREATE";
    private static final String ACTION_UPDATE = "UPDATE";
    private static final String ACTION_DELETE = "DELETE";
    private static final String ACTION_REORDER = "BATCH_SORT";

    private final CapabilityItemMapper capabilityItemMapper;
    private final CapabilityCategoryService capabilityCategoryService;
    private final AuditLogService auditLogService;
    private final PortalCacheInvalidationSupport portalCacheInvalidationSupport;

    public CapabilityItemServiceImpl(
            CapabilityItemMapper capabilityItemMapper,
            @Lazy CapabilityCategoryService capabilityCategoryService,
            AuditLogService auditLogService,
            PortalCacheInvalidationSupport portalCacheInvalidationSupport) {
        this.capabilityItemMapper = capabilityItemMapper;
        this.capabilityCategoryService = capabilityCategoryService;
        this.auditLogService = auditLogService;
        this.portalCacheInvalidationSupport = portalCacheInvalidationSupport;
    }

    @Override
    @Transactional
    public Long createItem(CapabilityItemCreateDTO dto) {
        // 校验所属分类必须存在且活跃
        if (!capabilityCategoryService.categoryExists(dto.getCategoryId())) {
            log.warn("create item category not found categoryId={}", dto.getCategoryId());
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_CATEGORY_NOT_FOUND);
        }

        String name = dto.getName().trim();
        validateNameUnique(null, dto.getCategoryId(), name);

        CapabilityItemEntity entity = new CapabilityItemEntity();
        entity.setCategoryId(dto.getCategoryId());
        entity.setName(name);
        entity.setVisible(dto.getVisible());
        entity.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 99);

        try {
            capabilityItemMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_ITEM_NAME_DUPLICATE);
        }

        log.info("create capability item success itemId={} name={}", entity.getId(), entity.getName());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateItem(Long id, CapabilityItemUpdateDTO dto) {
        CapabilityItemEntity entity = requireActiveItem(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), dto.getVersion());

        // 校验目标分类必须存在且活跃
        if (!capabilityCategoryService.categoryExists(dto.getCategoryId())) {
            log.warn("update item category not found categoryId={}", dto.getCategoryId());
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_CATEGORY_NOT_FOUND);
        }

        Map<String, Object> before = toSnapshot(entity);
        String newName = dto.getName().trim();
        validateNameUnique(id, dto.getCategoryId(), newName);

        entity.setCategoryId(dto.getCategoryId());
        entity.setName(newName);
        entity.setVisible(dto.getVisible());
        if (dto.getSortOrder() != null) {
            entity.setSortOrder(dto.getSortOrder());
        }

        try {
            ConcurrencyHelper.tryUpdate(capabilityItemMapper, entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_ITEM_NAME_DUPLICATE);
        }

        log.info("update capability item success itemId={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void deleteItem(Long id, Integer version) {
        CapabilityItemEntity entity = requireActiveItem(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);

        Map<String, Object> before = toSnapshot(entity);

        int deleted = capabilityItemMapper.delete(
                new LambdaUpdateWrapper<CapabilityItemEntity>()
                        .eq(CapabilityItemEntity::getId, entity.getId())
                        .eq(CapabilityItemEntity::getVersion, version));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }

        log.info("delete capability item success itemId={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void batchSortItems(List<CapabilityItemSortItemDTO> requestDTO) {
        if (requestDTO == null || requestDTO.isEmpty()) {
            log.warn("batch sort items received empty list");
            return;
        }

        List<Long> requestedIds = requestDTO.stream().map(CapabilityItemSortItemDTO::getId).toList();
        Set<Long> deduplicatedIds = new LinkedHashSet<>(requestedIds);
        if (deduplicatedIds.size() != requestedIds.size()) {
            log.warn("batch sort items duplicate ids detected requestedIds={}", requestedIds);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复的子项");
        }

        List<CapabilityItemEntity> activeItems = capabilityItemMapper.selectList(
                new LambdaQueryWrapper<CapabilityItemEntity>()
                        .eq(CapabilityItemEntity::getDeletedMarker, 0L)
                        .orderByAsc(CapabilityItemEntity::getId));
        if (activeItems.isEmpty()) {
            log.warn("no active items to sort");
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_ITEM_NOT_FOUND, "暂无可排序的子项");
        }

        Set<Long> currentIds = new LinkedHashSet<>(activeItems.stream().map(CapabilityItemEntity::getId).toList());
        if (!deduplicatedIds.equals(currentIds)) {
            log.warn("batch sort items completeness check failed requestedIds={} currentIds={}", deduplicatedIds, currentIds);
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前全部活跃底座子项");
        }

        Map<Long, CapabilityItemEntity> entityMap = new HashMap<>();
        for (CapabilityItemEntity item : activeItems) {
            entityMap.put(item.getId(), item);
        }

        List<Map<String, Object>> before = activeItems.stream()
                .sorted(Comparator.comparing(CapabilityItemEntity::getSortOrder).thenComparing(CapabilityItemEntity::getId))
                .map(this::toSnapshot)
                .toList();

        for (CapabilityItemSortItemDTO item : requestDTO) {
            CapabilityItemEntity entity = entityMap.get(item.getId());
            if (entity == null) {
                log.error("batch sort item not found in map itemId={}", item.getId());
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "子项已被删除，请刷新后重试");
            }
            entity.setSortOrder(item.getSortOrder());
            ConcurrencyHelper.tryUpdate(capabilityItemMapper, entity);
        }

        List<Map<String, Object>> after = requestDTO.stream()
                .map(item -> entityMap.get(item.getId()))
                .map(this::toSnapshot)
                .toList();

        log.info("batch sort items success count={}", requestDTO.size());
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void deleteItemsByCategoryId(Long categoryId) {
        log.info("cascade delete items by categoryId={}", categoryId);
        // 使用 MyBatis-Plus 的 @TableLogic 逻辑删除机制批量删除匹配的数据
        capabilityItemMapper.delete(
                new LambdaQueryWrapper<CapabilityItemEntity>()
                        .eq(CapabilityItemEntity::getCategoryId, categoryId));
    }

    private CapabilityItemEntity requireActiveItem(Long id) {
        CapabilityItemEntity entity = capabilityItemMapper.selectOne(
                new LambdaQueryWrapper<CapabilityItemEntity>()
                        .eq(CapabilityItemEntity::getId, id)
                        .eq(CapabilityItemEntity::getDeletedMarker, 0L));
        if (entity == null) {
            log.warn("capability item not found itemId={}", id);
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_ITEM_NOT_FOUND);
        }
        return entity;
    }

    private void validateNameUnique(Long excludeId, Long categoryId, String name) {
        LambdaQueryWrapper<CapabilityItemEntity> queryWrapper = new LambdaQueryWrapper<CapabilityItemEntity>()
                .eq(CapabilityItemEntity::getCategoryId, categoryId)
                .eq(CapabilityItemEntity::getName, name)
                .eq(CapabilityItemEntity::getDeletedMarker, 0L);
        if (excludeId != null) {
            queryWrapper.ne(CapabilityItemEntity::getId, excludeId);
        }
        Long count = capabilityItemMapper.selectCount(queryWrapper);
        if (count != null && count > 0) {
            log.warn("capability item name duplicate name={} categoryId={}", name, categoryId);
            throw new BusinessException(ErrorCode.SITE_CAPABILITY_ITEM_NAME_DUPLICATE);
        }
    }

    private void invalidatePortalCache() {
        portalCacheInvalidationSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }

    private Map<String, Object> toSnapshot(CapabilityItemEntity entity) {
        if (entity == null) {
            return null;
        }
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("categoryId", entity.getCategoryId());
        snapshot.put("name", entity.getName());
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }
}
