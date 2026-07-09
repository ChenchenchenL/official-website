package com.company.officialwebsite.modules.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryUpdateRequestDTO;
import com.company.officialwebsite.modules.business.entity.BusinessRegistryEntity;
import com.company.officialwebsite.modules.business.mapper.BusinessRegistryMapper;
import com.company.officialwebsite.modules.business.service.BusinessRegistryService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessRegistryVO;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessRegistryServiceImpl implements BusinessRegistryService {

    private static final Logger log = LoggerFactory.getLogger(BusinessRegistryServiceImpl.class);

    private static final String BIZ_MODULE = "BUSINESS";
    private static final String TARGET_TYPE = "BUSINESS_REGISTRY";
    private static final String ACTION_CREATE = "CREATE_BUSINESS_REGISTRY";
    private static final String ACTION_UPDATE = "UPDATE_BUSINESS_REGISTRY";
    private static final String ACTION_DELETE = "DELETE_BUSINESS_REGISTRY";
    private static final String ACTION_REORDER = "REORDER_BUSINESS_REGISTRY";
    private static final String MEDIA_BIZ_FIELD = "icon";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ONLINE = "ONLINE";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String MSG_NOT_FOUND = "Business registry does not exist or has been deleted";
    private static final String MSG_DUPLICATE = "Business code already exists";
    private static final String MSG_BUSINESS_CODE_REQUIRED = "Business code cannot be empty";
    private static final String MSG_BUSINESS_NAME_REQUIRED = "Business name cannot be empty";
    private static final String MSG_STATUS_INVALID = "Business status must be DRAFT, ONLINE, or OFFLINE";
    private static final String MSG_EMPTY_ORDERED_IDS = "Ordered business id list cannot be empty";
    private static final String MSG_INVALID_ORDERED_IDS = "Ordered business id list contains invalid businesses";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "Ordered business id list must cover all active businesses";
    private static final String MSG_ORDERED_ID_REQUIRED = "Ordered business id cannot be empty";
    private static final String MSG_SORT_ORDER_LIMIT = "Business sort order has reached the limit";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "Business sort order is out of range";

    private final BusinessRegistryMapper businessRegistryMapper;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final OfficialProperties officialProperties;

    public BusinessRegistryServiceImpl(
            BusinessRegistryMapper businessRegistryMapper,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            OfficialProperties officialProperties) {
        this.businessRegistryMapper = businessRegistryMapper;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminBusinessRegistryVO> getAdminBusinessRegistryList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        Page<BusinessRegistryEntity> page = businessRegistryMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<BusinessRegistryEntity>()
                        .eq(BusinessRegistryEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessRegistryEntity::getSortOrder)
                        .orderByAsc(BusinessRegistryEntity::getId));
        List<AdminBusinessRegistryVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createBusinessRegistry(BusinessRegistryCreateRequestDTO requestDTO) {
        BusinessRegistryEntity entity = new BusinessRegistryEntity();
        applyCreateRequest(entity, requestDTO);
        validateIcon(entity.getIconMediaId());

        try {
            businessRegistryMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create business registry duplicate businessCode={}", entity.getBusinessCode(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        bindIcon(entity);
        log.info("create business registry success id={} businessCode={}", entity.getId(), entity.getBusinessCode());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void updateBusinessRegistry(Long id, BusinessRegistryUpdateRequestDTO requestDTO) {
        BusinessRegistryEntity entity = requireActiveBusiness(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        Long oldIconMediaId = entity.getIconMediaId();

        applyUpdateRequest(entity, requestDTO);
        validateIcon(entity.getIconMediaId());

        try {
            ConcurrencyHelper.tryUpdate(businessRegistryMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update business registry duplicate id={} businessCode={}", entity.getId(), entity.getBusinessCode(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        if (!Objects.equals(oldIconMediaId, entity.getIconMediaId())) {
            bindIcon(entity);
        }
        log.info("update business registry success id={} businessCode={}", entity.getId(), entity.getBusinessCode());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void deleteBusinessRegistry(Long id, Integer version) {
        BusinessRegistryEntity entity = requireActiveBusiness(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = businessRegistryMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        mediaAssetService.bindMedia(null, BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        log.info("delete business registry success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    @Override
    @Transactional
    public void reorderBusinessRegistry(BusinessRegistryBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedBusinessIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_ORDERED_IDS);
        }

        List<BusinessRegistryEntity> activeBusinesses = listActiveBusinesses();
        if (activeBusinesses.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }

        Map<Long, BusinessRegistryEntity> entityMap = new HashMap<>();
        for (BusinessRegistryEntity business : activeBusinesses) {
            entityMap.put(business.getId(), business);
        }
        if (!entityMap.keySet().equals(new LinkedHashSet<>(requestedOrder))) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }

        List<Map<String, Object>> before = activeBusinesses.stream()
                .sorted(businessComparator())
                .map(this::toSnapshot)
                .toList();

        int orderIndex = 0;
        for (Long id : requestedOrder) {
            BusinessRegistryEntity entity = entityMap.get(id);
            entity.setSortOrder(sortOrderForIndex(orderIndex));
            ConcurrencyHelper.tryUpdate(businessRegistryMapper, entity);
            orderIndex++;
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();
        log.info("reorder business registry success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
    }

    private BusinessRegistryEntity requireActiveBusiness(Long id) {
        BusinessRegistryEntity entity = businessRegistryMapper.selectOne(
                new LambdaQueryWrapper<BusinessRegistryEntity>()
                        .eq(BusinessRegistryEntity::getId, id)
                        .eq(BusinessRegistryEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("business registry not found id={}", id);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_NOT_FOUND);
        }
        return entity;
    }

    private List<BusinessRegistryEntity> listActiveBusinesses() {
        return businessRegistryMapper.selectList(
                new LambdaQueryWrapper<BusinessRegistryEntity>()
                        .eq(BusinessRegistryEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessRegistryEntity::getSortOrder)
                        .orderByAsc(BusinessRegistryEntity::getId));
    }

    private void applyCreateRequest(BusinessRegistryEntity entity, BusinessRegistryCreateRequestDTO requestDTO) {
        entity.setBusinessCode(normalizeCode(requestDTO.getBusinessCode()));
        entity.setBusinessName(normalizeName(requestDTO.getBusinessName()));
        entity.setIconMediaId(requestDTO.getIconMediaId());
        entity.setDescription(StringFieldUtils.defaultString(requestDTO.getDescription()).trim());
        entity.setBusinessStatus(normalizeStatus(requestDTO.getBusinessStatus(), STATUS_DRAFT));
        entity.setSortOrder(requestDTO.getSortOrder() == null ? nextSortOrder() : requestDTO.getSortOrder());
    }

    private void applyUpdateRequest(BusinessRegistryEntity entity, BusinessRegistryUpdateRequestDTO requestDTO) {
        entity.setBusinessCode(normalizeCode(requestDTO.getBusinessCode()));
        entity.setBusinessName(normalizeName(requestDTO.getBusinessName()));
        entity.setIconMediaId(requestDTO.getIconMediaId());
        entity.setDescription(StringFieldUtils.defaultString(requestDTO.getDescription()).trim());
        entity.setBusinessStatus(normalizeStatus(requestDTO.getBusinessStatus(), entity.getBusinessStatus()));
        entity.setSortOrder(requestDTO.getSortOrder() == null ? entity.getSortOrder() : requestDTO.getSortOrder());
    }

    private void validateIcon(Long iconMediaId) {
        if (iconMediaId != null) {
            mediaAssetService.requireUsableImage(iconMediaId);
        }
    }

    private void bindIcon(BusinessRegistryEntity entity) {
        mediaAssetService.bindMedia(entity.getIconMediaId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
    }

    private AdminBusinessRegistryVO toAdminVO(BusinessRegistryEntity entity) {
        AdminBusinessRegistryVO vo = new AdminBusinessRegistryVO();
        vo.setId(entity.getId());
        vo.setBusinessCode(StringFieldUtils.defaultString(entity.getBusinessCode()));
        vo.setBusinessName(StringFieldUtils.defaultString(entity.getBusinessName()));
        vo.setIconMediaId(entity.getIconMediaId());
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setBusinessStatus(StringFieldUtils.defaultString(entity.getBusinessStatus()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());

        MediaAssetEntity asset = resolveIconAsset(entity.getIconMediaId(), entity.getId());
        if (asset != null) {
            AdminBusinessRegistryVO.IconVO icon = new AdminBusinessRegistryVO.IconVO();
            icon.setId(entity.getIconMediaId());
            icon.setUrl(StringFieldUtils.defaultString(asset.getPublicUrl()));
            icon.setFileName(StringFieldUtils.defaultString(asset.getOriginalFilename()));
            vo.setIcon(icon);
            vo.setIconUrl(icon.getUrl());
        } else {
            vo.setIconUrl("");
        }
        return vo;
    }

    private MediaAssetEntity resolveIconAsset(Long iconMediaId, Long businessId) {
        if (iconMediaId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(iconMediaId);
        } catch (BusinessException ex) {
            log.warn("business icon unavailable businessId={} iconMediaId={}", businessId, iconMediaId);
            return null;
        }
    }

    private Map<String, Object> toSnapshot(BusinessRegistryEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("businessCode", entity.getBusinessCode());
        snapshot.put("businessName", entity.getBusinessName());
        snapshot.put("iconMediaId", entity.getIconMediaId());
        snapshot.put("description", entity.getDescription());
        snapshot.put("businessStatus", entity.getBusinessStatus());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private String normalizeCode(String value) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_BUSINESS_CODE_REQUIRED);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String value) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_BUSINESS_NAME_REQUIRED);
        }
        return normalized;
    }

    private String normalizeStatus(String status, String defaultStatus) {
        String normalized = status == null || status.isBlank() ? defaultStatus : status.trim().toUpperCase(Locale.ROOT);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_STATUS_INVALID);
        }
        if (!Set.of(STATUS_DRAFT, STATUS_ONLINE, STATUS_OFFLINE).contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_STATUS_INVALID);
        }
        return normalized;
    }

    private int nextSortOrder() {
        BusinessRegistryEntity last = businessRegistryMapper.selectOne(
                new LambdaQueryWrapper<BusinessRegistryEntity>()
                        .eq(BusinessRegistryEntity::getDeletedMarker, 0L)
                        .orderByDesc(BusinessRegistryEntity::getSortOrder)
                        .orderByDesc(BusinessRegistryEntity::getId)
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

    private Comparator<BusinessRegistryEntity> businessComparator() {
        return Comparator
                .comparing(BusinessRegistryEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BusinessRegistryEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private int sortGap() {
        return Math.max(1, officialProperties.getCache().getSortGap());
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
