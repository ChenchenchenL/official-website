package com.company.officialwebsite.modules.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.business.dto.BusinessPageBlockBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageBlockCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageBlockUpdateRequestDTO;
import com.company.officialwebsite.modules.business.entity.BusinessBlockEntity;
import com.company.officialwebsite.modules.business.entity.BusinessPageBlockEntity;
import com.company.officialwebsite.modules.business.entity.BusinessPageEntity;
import com.company.officialwebsite.modules.business.mapper.BusinessBlockMapper;
import com.company.officialwebsite.modules.business.mapper.BusinessPageBlockMapper;
import com.company.officialwebsite.modules.business.mapper.BusinessPageMapper;
import com.company.officialwebsite.modules.business.service.BusinessPageBlockService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessPageBlockVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessPageBlockServiceImpl implements BusinessPageBlockService {

    private static final Logger log = LoggerFactory.getLogger(BusinessPageBlockServiceImpl.class);

    private static final String BIZ_MODULE = "BUSINESS";
    private static final String TARGET_TYPE = "BUSINESS_PAGE_BLOCK";
    private static final String ACTION_CREATE = "CREATE_BUSINESS_PAGE_BLOCK";
    private static final String ACTION_UPDATE = "UPDATE_BUSINESS_PAGE_BLOCK";
    private static final String ACTION_DELETE = "DELETE_BUSINESS_PAGE_BLOCK";
    private static final String ACTION_REORDER = "REORDER_BUSINESS_PAGE_BLOCK";
    private static final String MSG_NOT_FOUND = "Business page block does not exist or has been deleted";
    private static final String MSG_PAGE_NOT_FOUND = "Business page does not exist or has been deleted";
    private static final String MSG_BLOCK_NOT_FOUND = "Business block does not exist or has been deleted";
    private static final String MSG_EMPTY_ORDERED_IDS = "Ordered page block id list cannot be empty";
    private static final String MSG_INVALID_ORDERED_IDS = "Ordered page block id list contains invalid page blocks";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "Ordered page block id list must cover all active page blocks";
    private static final String MSG_ORDERED_ID_REQUIRED = "Ordered page block id cannot be empty";
    private static final String MSG_SORT_ORDER_LIMIT = "Page block sort order has reached the limit";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "Page block sort order is out of range";

    private final BusinessPageBlockMapper businessPageBlockMapper;
    private final BusinessPageMapper businessPageMapper;
    private final BusinessBlockMapper businessBlockMapper;
    private final AuditLogService auditLogService;
    private final OfficialProperties officialProperties;

    public BusinessPageBlockServiceImpl(
            BusinessPageBlockMapper businessPageBlockMapper,
            BusinessPageMapper businessPageMapper,
            BusinessBlockMapper businessBlockMapper,
            AuditLogService auditLogService,
            OfficialProperties officialProperties) {
        this.businessPageBlockMapper = businessPageBlockMapper;
        this.businessPageMapper = businessPageMapper;
        this.businessBlockMapper = businessBlockMapper;
        this.auditLogService = auditLogService;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminBusinessPageBlockVO> getAdminBusinessPageBlockList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        Page<BusinessPageBlockEntity> page = businessPageBlockMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<BusinessPageBlockEntity>()
                        .eq(BusinessPageBlockEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessPageBlockEntity::getPageId)
                        .orderByAsc(BusinessPageBlockEntity::getSortOrder)
                        .orderByAsc(BusinessPageBlockEntity::getId));
        List<AdminBusinessPageBlockVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createBusinessPageBlock(BusinessPageBlockCreateRequestDTO requestDTO) {
        BusinessPageBlockEntity entity = new BusinessPageBlockEntity();
        applyCreateRequest(entity, requestDTO);
        businessPageBlockMapper.insert(entity);
        log.info("create business page block success id={} pageId={} blockId={}", entity.getId(), entity.getPageId(), entity.getBlockId());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void updateBusinessPageBlock(Long id, BusinessPageBlockUpdateRequestDTO requestDTO) {
        BusinessPageBlockEntity entity = requireActivePageBlock(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        applyUpdateRequest(entity, requestDTO);
        ConcurrencyHelper.tryUpdate(businessPageBlockMapper, entity);
        log.info("update business page block success id={} pageId={} blockId={}", entity.getId(), entity.getPageId(), entity.getBlockId());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void deleteBusinessPageBlock(Long id, Integer version) {
        BusinessPageBlockEntity entity = requireActivePageBlock(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = businessPageBlockMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete business page block success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    @Override
    @Transactional
    public void reorderBusinessPageBlocks(BusinessPageBlockBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedPageBlockIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_ORDERED_IDS);
        }
        List<BusinessPageBlockEntity> activePageBlocks = listActivePageBlocks();
        if (activePageBlocks.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }
        Map<Long, BusinessPageBlockEntity> entityMap = new HashMap<>();
        for (BusinessPageBlockEntity pageBlock : activePageBlocks) {
            entityMap.put(pageBlock.getId(), pageBlock);
        }
        if (!entityMap.keySet().equals(new LinkedHashSet<>(requestedOrder))) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }
        List<Map<String, Object>> before = activePageBlocks.stream()
                .sorted(pageBlockComparator())
                .map(this::toSnapshot)
                .toList();
        for (int index = 0; index < requestedOrder.size(); index++) {
            BusinessPageBlockEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            ConcurrencyHelper.tryUpdate(businessPageBlockMapper, entity);
        }
        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();
        log.info("reorder business page block success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
    }

    private BusinessPageBlockEntity requireActivePageBlock(Long id) {
        BusinessPageBlockEntity entity = businessPageBlockMapper.selectOne(
                new LambdaQueryWrapper<BusinessPageBlockEntity>()
                        .eq(BusinessPageBlockEntity::getId, id)
                        .eq(BusinessPageBlockEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_NOT_FOUND);
        }
        return entity;
    }

    private BusinessPageEntity requireActivePage(Long id) {
        BusinessPageEntity entity = businessPageMapper.selectOne(
                new LambdaQueryWrapper<BusinessPageEntity>()
                        .eq(BusinessPageEntity::getId, id)
                        .eq(BusinessPageEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_PAGE_NOT_FOUND);
        }
        return entity;
    }

    private BusinessBlockEntity requireActiveBlock(Long id) {
        BusinessBlockEntity entity = businessBlockMapper.selectOne(
                new LambdaQueryWrapper<BusinessBlockEntity>()
                        .eq(BusinessBlockEntity::getId, id)
                        .eq(BusinessBlockEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_BLOCK_NOT_FOUND);
        }
        return entity;
    }

    private List<BusinessPageBlockEntity> listActivePageBlocks() {
        return businessPageBlockMapper.selectList(
                new LambdaQueryWrapper<BusinessPageBlockEntity>()
                        .eq(BusinessPageBlockEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessPageBlockEntity::getPageId)
                        .orderByAsc(BusinessPageBlockEntity::getSortOrder)
                        .orderByAsc(BusinessPageBlockEntity::getId));
    }

    private void applyCreateRequest(BusinessPageBlockEntity entity, BusinessPageBlockCreateRequestDTO requestDTO) {
        requireActivePage(requestDTO.getPageId());
        BusinessBlockEntity block = requireActiveBlock(requestDTO.getBlockId());
        entity.setPageId(requestDTO.getPageId());
        entity.setBlockId(block.getId());
        entity.setBlockCode(block.getBlockCode());
        entity.setBlockName(block.getBlockName());
        entity.setBlockType(block.getBlockType());
        entity.setBlockConfig(normalizeBlockConfig(requestDTO.getBlockConfig(), block.getDefaultConfig()));
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? nextSortOrder() : requestDTO.getSortOrder());
    }

    private void applyUpdateRequest(BusinessPageBlockEntity entity, BusinessPageBlockUpdateRequestDTO requestDTO) {
        requireActivePage(requestDTO.getPageId());
        BusinessBlockEntity block = requireActiveBlock(requestDTO.getBlockId());
        entity.setPageId(requestDTO.getPageId());
        entity.setBlockId(block.getId());
        entity.setBlockCode(block.getBlockCode());
        entity.setBlockName(block.getBlockName());
        entity.setBlockType(block.getBlockType());
        entity.setBlockConfig(normalizeBlockConfig(requestDTO.getBlockConfig(), block.getDefaultConfig()));
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? entity.getSortOrder() : requestDTO.getSortOrder());
    }

    private AdminBusinessPageBlockVO toAdminVO(BusinessPageBlockEntity entity) {
        AdminBusinessPageBlockVO vo = new AdminBusinessPageBlockVO();
        vo.setId(entity.getId());
        vo.setPageId(entity.getPageId());
        vo.setBlockId(entity.getBlockId());
        vo.setBlockCode(StringFieldUtils.defaultString(entity.getBlockCode()));
        vo.setBlockName(StringFieldUtils.defaultString(entity.getBlockName()));
        vo.setBlockType(StringFieldUtils.defaultString(entity.getBlockType()));
        vo.setBlockConfig(StringFieldUtils.defaultString(entity.getBlockConfig()));
        vo.setVisible(entity.getVisible() == null || entity.getVisible());
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        BusinessPageEntity page = entity.getPageId() == null ? null : businessPageMapper.selectById(entity.getPageId());
        vo.setPageName(page == null ? "" : StringFieldUtils.defaultString(page.getPageName()));
        return vo;
    }

    private Map<String, Object> toSnapshot(BusinessPageBlockEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("pageId", entity.getPageId());
        snapshot.put("blockId", entity.getBlockId());
        snapshot.put("blockCode", entity.getBlockCode());
        snapshot.put("blockName", entity.getBlockName());
        snapshot.put("blockType", entity.getBlockType());
        snapshot.put("blockConfig", entity.getBlockConfig());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private String normalizeBlockConfig(String value, String defaultValue) {
        String normalized = StringFieldUtils.trimToNull(value);
        return normalized == null ? StringFieldUtils.defaultString(defaultValue).trim() : normalized;
    }

    private int nextSortOrder() {
        BusinessPageBlockEntity last = businessPageBlockMapper.selectOne(
                new LambdaQueryWrapper<BusinessPageBlockEntity>()
                        .eq(BusinessPageBlockEntity::getDeletedMarker, 0L)
                        .orderByDesc(BusinessPageBlockEntity::getSortOrder)
                        .orderByDesc(BusinessPageBlockEntity::getId)
                        .last("limit 1"));
        int current = last == null || last.getSortOrder() == null ? 0 : last.getSortOrder();
        int sortGap = sortGap();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_SORT_ORDER_LIMIT);
        }
        return current + sortGap;
    }

    private int sortOrderForIndex(int index) {
        try {
            return Math.multiplyExact(Math.addExact(index, 1), sortGap());
        } catch (ArithmeticException ex) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_SORT_ORDER_OUT_OF_RANGE);
        }
    }

    private List<Long> deduplicateIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_ORDERED_ID_REQUIRED);
            }
            deduplicated.add(id);
        }
        return List.copyOf(deduplicated);
    }

    private Comparator<BusinessPageBlockEntity> pageBlockComparator() {
        return Comparator
                .comparing(BusinessPageBlockEntity::getPageId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(BusinessPageBlockEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BusinessPageBlockEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private int sortGap() {
        return Math.max(1, officialProperties.getCache().getSortGap());
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
