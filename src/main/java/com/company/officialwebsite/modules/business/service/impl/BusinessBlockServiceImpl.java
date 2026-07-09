package com.company.officialwebsite.modules.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.business.dto.BusinessBlockBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessBlockCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessBlockUpdateRequestDTO;
import com.company.officialwebsite.modules.business.entity.BusinessBlockEntity;
import com.company.officialwebsite.modules.business.mapper.BusinessBlockMapper;
import com.company.officialwebsite.modules.business.service.BusinessBlockService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessBlockVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessBlockServiceImpl implements BusinessBlockService {

    private static final Logger log = LoggerFactory.getLogger(BusinessBlockServiceImpl.class);

    private static final String BIZ_MODULE = "BUSINESS";
    private static final String TARGET_TYPE = "BUSINESS_BLOCK";
    private static final String ACTION_CREATE = "CREATE_BUSINESS_BLOCK";
    private static final String ACTION_UPDATE = "UPDATE_BUSINESS_BLOCK";
    private static final String ACTION_DELETE = "DELETE_BUSINESS_BLOCK";
    private static final String ACTION_REORDER = "REORDER_BUSINESS_BLOCK";
    private static final String MSG_NOT_FOUND = "Business block does not exist or has been deleted";
    private static final String MSG_DUPLICATE = "Business block code already exists";
    private static final String MSG_BLOCK_CODE_REQUIRED = "Block code cannot be empty";
    private static final String MSG_BLOCK_NAME_REQUIRED = "Block name cannot be empty";
    private static final String MSG_BLOCK_TYPE_REQUIRED = "Block type cannot be empty";
    private static final String MSG_BLOCK_TYPE_INVALID = "Block type must be HERO, STATS, PRODUCT_LIST, CASE_LIST, CAPABILITY_CARDS, CONTACT, CONTACT_US, or CTA";
    private static final String MSG_EMPTY_ORDERED_IDS = "Ordered block id list cannot be empty";
    private static final String MSG_INVALID_ORDERED_IDS = "Ordered block id list contains invalid blocks";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "Ordered block id list must cover all active blocks";
    private static final String MSG_ORDERED_ID_REQUIRED = "Ordered block id cannot be empty";
    private static final String MSG_SORT_ORDER_LIMIT = "Block sort order has reached the limit";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "Block sort order is out of range";

    private final BusinessBlockMapper businessBlockMapper;
    private final AuditLogService auditLogService;
    private final OfficialProperties officialProperties;

    public BusinessBlockServiceImpl(
            BusinessBlockMapper businessBlockMapper,
            AuditLogService auditLogService,
            OfficialProperties officialProperties) {
        this.businessBlockMapper = businessBlockMapper;
        this.auditLogService = auditLogService;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminBusinessBlockVO> getAdminBusinessBlockList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        Page<BusinessBlockEntity> page = businessBlockMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<BusinessBlockEntity>()
                        .eq(BusinessBlockEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessBlockEntity::getSortOrder)
                        .orderByAsc(BusinessBlockEntity::getId));
        List<AdminBusinessBlockVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createBusinessBlock(BusinessBlockCreateRequestDTO requestDTO) {
        BusinessBlockEntity entity = new BusinessBlockEntity();
        applyCreateRequest(entity, requestDTO);
        try {
            businessBlockMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create business block duplicate blockCode={}", entity.getBlockCode(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }
        log.info("create business block success id={} blockCode={}", entity.getId(), entity.getBlockCode());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void updateBusinessBlock(Long id, BusinessBlockUpdateRequestDTO requestDTO) {
        BusinessBlockEntity entity = requireActiveBlock(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        applyUpdateRequest(entity, requestDTO);
        try {
            ConcurrencyHelper.tryUpdate(businessBlockMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update business block duplicate id={} blockCode={}", entity.getId(), entity.getBlockCode(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }
        log.info("update business block success id={} blockCode={}", entity.getId(), entity.getBlockCode());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void deleteBusinessBlock(Long id, Integer version) {
        BusinessBlockEntity entity = requireActiveBlock(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = businessBlockMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete business block success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    @Override
    @Transactional
    public void reorderBusinessBlocks(BusinessBlockBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedBlockIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_ORDERED_IDS);
        }

        List<BusinessBlockEntity> activeBlocks = listActiveBlocks();
        if (activeBlocks.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }

        Map<Long, BusinessBlockEntity> entityMap = new HashMap<>();
        for (BusinessBlockEntity block : activeBlocks) {
            entityMap.put(block.getId(), block);
        }
        if (!entityMap.keySet().equals(new LinkedHashSet<>(requestedOrder))) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }

        List<Map<String, Object>> before = activeBlocks.stream()
                .sorted(blockComparator())
                .map(this::toSnapshot)
                .toList();

        for (int index = 0; index < requestedOrder.size(); index++) {
            BusinessBlockEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            ConcurrencyHelper.tryUpdate(businessBlockMapper, entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();
        log.info("reorder business block success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
    }

    private BusinessBlockEntity requireActiveBlock(Long id) {
        BusinessBlockEntity entity = businessBlockMapper.selectOne(
                new LambdaQueryWrapper<BusinessBlockEntity>()
                        .eq(BusinessBlockEntity::getId, id)
                        .eq(BusinessBlockEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("business block not found id={}", id);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_NOT_FOUND);
        }
        return entity;
    }

    private List<BusinessBlockEntity> listActiveBlocks() {
        return businessBlockMapper.selectList(
                new LambdaQueryWrapper<BusinessBlockEntity>()
                        .eq(BusinessBlockEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessBlockEntity::getSortOrder)
                        .orderByAsc(BusinessBlockEntity::getId));
    }

    private void applyCreateRequest(BusinessBlockEntity entity, BusinessBlockCreateRequestDTO requestDTO) {
        entity.setBlockCode(normalizeCode(requestDTO.getBlockCode(), MSG_BLOCK_CODE_REQUIRED));
        entity.setBlockName(normalizeName(requestDTO.getBlockName(), MSG_BLOCK_NAME_REQUIRED));
        entity.setBlockType(normalizeBlockType(requestDTO.getBlockType()));
        entity.setDescription(StringFieldUtils.defaultString(requestDTO.getDescription()).trim());
        entity.setDefaultConfig(StringFieldUtils.defaultString(requestDTO.getDefaultConfig()).trim());
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? nextSortOrder() : requestDTO.getSortOrder());
    }

    private void applyUpdateRequest(BusinessBlockEntity entity, BusinessBlockUpdateRequestDTO requestDTO) {
        entity.setBlockCode(normalizeCode(requestDTO.getBlockCode(), MSG_BLOCK_CODE_REQUIRED));
        entity.setBlockName(normalizeName(requestDTO.getBlockName(), MSG_BLOCK_NAME_REQUIRED));
        entity.setBlockType(normalizeBlockType(requestDTO.getBlockType()));
        entity.setDescription(StringFieldUtils.defaultString(requestDTO.getDescription()).trim());
        entity.setDefaultConfig(StringFieldUtils.defaultString(requestDTO.getDefaultConfig()).trim());
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? entity.getSortOrder() : requestDTO.getSortOrder());
    }

    private AdminBusinessBlockVO toAdminVO(BusinessBlockEntity entity) {
        AdminBusinessBlockVO vo = new AdminBusinessBlockVO();
        vo.setId(entity.getId());
        vo.setBlockCode(StringFieldUtils.defaultString(entity.getBlockCode()));
        vo.setBlockName(StringFieldUtils.defaultString(entity.getBlockName()));
        vo.setBlockType(StringFieldUtils.defaultString(entity.getBlockType()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setDefaultConfig(StringFieldUtils.defaultString(entity.getDefaultConfig()));
        vo.setVisible(entity.getVisible() == null || entity.getVisible());
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Map<String, Object> toSnapshot(BusinessBlockEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("blockCode", entity.getBlockCode());
        snapshot.put("blockName", entity.getBlockName());
        snapshot.put("blockType", entity.getBlockType());
        snapshot.put("description", entity.getDescription());
        snapshot.put("defaultConfig", entity.getDefaultConfig());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private String normalizeCode(String value, String blankMessage) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, blankMessage);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String value, String blankMessage) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, blankMessage);
        }
        return normalized;
    }

    private String normalizeBlockType(String value) {
        String normalized = normalizeCode(value, MSG_BLOCK_TYPE_REQUIRED);
        if (!Set.of("HERO", "STATS", "PRODUCT_LIST", "CASE_LIST", "CAPABILITY_CARDS", "CONTACT", "CONTACT_US", "CTA").contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_BLOCK_TYPE_INVALID);
        }
        return normalized;
    }

    private int nextSortOrder() {
        BusinessBlockEntity last = businessBlockMapper.selectOne(
                new LambdaQueryWrapper<BusinessBlockEntity>()
                        .eq(BusinessBlockEntity::getDeletedMarker, 0L)
                        .orderByDesc(BusinessBlockEntity::getSortOrder)
                        .orderByDesc(BusinessBlockEntity::getId)
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

    private Comparator<BusinessBlockEntity> blockComparator() {
        return Comparator
                .comparing(BusinessBlockEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BusinessBlockEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private int sortGap() {
        return Math.max(1, officialProperties.getCache().getSortGap());
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
