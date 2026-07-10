package com.company.officialwebsite.modules.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateCreateBusinessRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateUpdateRequestDTO;
import com.company.officialwebsite.modules.business.entity.BusinessTemplateEntity;
import com.company.officialwebsite.modules.business.mapper.BusinessTemplateMapper;
import com.company.officialwebsite.modules.business.service.BusinessRegistryService;
import com.company.officialwebsite.modules.business.service.BusinessTemplateService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessTemplateVO;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
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
public class BusinessTemplateServiceImpl implements BusinessTemplateService {

    private static final Logger log = LoggerFactory.getLogger(BusinessTemplateServiceImpl.class);

    private static final String BIZ_MODULE = "BUSINESS";
    private static final String TARGET_TYPE = "BUSINESS_TEMPLATE";
    private static final String ACTION_CREATE = "CREATE_BUSINESS_TEMPLATE";
    private static final String ACTION_UPDATE = "UPDATE_BUSINESS_TEMPLATE";
    private static final String ACTION_DELETE = "DELETE_BUSINESS_TEMPLATE";
    private static final String ACTION_COPY = "COPY_BUSINESS_TEMPLATE";
    private static final String ACTION_CREATE_BUSINESS = "CREATE_BUSINESS_FROM_TEMPLATE";
    private static final String ACTION_REORDER = "REORDER_BUSINESS_TEMPLATE";
    private static final String MEDIA_BIZ_FIELD = "defaultIcon";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ONLINE = "ONLINE";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String MSG_NOT_FOUND = "Business template does not exist or has been deleted";
    private static final String MSG_DUPLICATE = "Business template code already exists";
    private static final String MSG_TEMPLATE_CODE_REQUIRED = "Template code cannot be empty";
    private static final String MSG_TEMPLATE_NAME_REQUIRED = "Template name cannot be empty";
    private static final String MSG_TEMPLATE_TYPE_REQUIRED = "Template type cannot be empty";
    private static final String MSG_BUSINESS_CODE_REQUIRED = "Business code cannot be empty";
    private static final String MSG_BUSINESS_NAME_REQUIRED = "Business name cannot be empty";
    private static final String MSG_STATUS_INVALID = "Business status must be DRAFT, ONLINE, or OFFLINE";
    private static final String MSG_EMPTY_ORDERED_IDS = "Ordered template id list cannot be empty";
    private static final String MSG_INVALID_ORDERED_IDS = "Ordered template id list contains invalid templates";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "Ordered template id list must cover all active templates";
    private static final String MSG_ORDERED_ID_REQUIRED = "Ordered template id cannot be empty";
    private static final String MSG_SORT_ORDER_LIMIT = "Template sort order has reached the limit";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "Template sort order is out of range";
    private static final int MAX_CODE_LENGTH = 64;
    private static final int MAX_COPY_NAME_LENGTH = 128;

    private final BusinessTemplateMapper businessTemplateMapper;
    private final BusinessRegistryService businessRegistryService;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final OfficialProperties officialProperties;

    public BusinessTemplateServiceImpl(
            BusinessTemplateMapper businessTemplateMapper,
            BusinessRegistryService businessRegistryService,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            OfficialProperties officialProperties) {
        this.businessTemplateMapper = businessTemplateMapper;
        this.businessRegistryService = businessRegistryService;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminBusinessTemplateVO> getAdminBusinessTemplateList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        Page<BusinessTemplateEntity> page = businessTemplateMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<BusinessTemplateEntity>()
                        .eq(BusinessTemplateEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessTemplateEntity::getSortOrder)
                        .orderByAsc(BusinessTemplateEntity::getId));
        List<AdminBusinessTemplateVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createBusinessTemplate(BusinessTemplateCreateRequestDTO requestDTO) {
        BusinessTemplateEntity entity = new BusinessTemplateEntity();
        applyCreateRequest(entity, requestDTO);
        validateIcon(entity.getDefaultIconMediaId());
        try {
            businessTemplateMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create business template duplicate templateCode={}", entity.getTemplateCode(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }
        bindIcon(entity);
        log.info("create business template success id={} templateCode={}", entity.getId(), entity.getTemplateCode());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void updateBusinessTemplate(Long id, BusinessTemplateUpdateRequestDTO requestDTO) {
        BusinessTemplateEntity entity = requireActiveTemplate(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        applyUpdateRequest(entity, requestDTO);
        validateIcon(entity.getDefaultIconMediaId());
        try {
            ConcurrencyHelper.tryUpdate(businessTemplateMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update business template duplicate id={} templateCode={}", entity.getId(), entity.getTemplateCode(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }
        bindIcon(entity);
        log.info("update business template success id={} templateCode={}", entity.getId(), entity.getTemplateCode());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void deleteBusinessTemplate(Long id, Integer version) {
        BusinessTemplateEntity entity = requireActiveTemplate(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = businessTemplateMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        mediaAssetService.bindMedia(null, TARGET_TYPE, entity.getId(), MEDIA_BIZ_FIELD);
        log.info("delete business template success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    @Override
    @Transactional
    public void copyBusinessTemplate(Long id, Integer version) {
        BusinessTemplateEntity source = requireActiveTemplate(id);
        ConcurrencyHelper.assertVersion(source.getVersion(), version);
        BusinessTemplateEntity copy = new BusinessTemplateEntity();
        copy.setTemplateCode(nextCopyCode(source.getTemplateCode()));
        copy.setTemplateName(truncate(source.getTemplateName() + " Copy", MAX_COPY_NAME_LENGTH));
        copy.setTemplateType(source.getTemplateType());
        copy.setDefaultBusinessCode(source.getDefaultBusinessCode());
        copy.setDefaultBusinessName(source.getDefaultBusinessName());
        copy.setDefaultIconMediaId(source.getDefaultIconMediaId());
        copy.setDefaultBusinessStatus(source.getDefaultBusinessStatus());
        copy.setDescription(source.getDescription());
        copy.setTemplateConfig(source.getTemplateConfig());
        copy.setSortOrder(nextSortOrder());
        businessTemplateMapper.insert(copy);
        bindIcon(copy);
        log.info("copy business template success sourceId={} copyId={}", source.getId(), copy.getId());
        recordAudit(ACTION_COPY, copy.getId(), toSnapshot(source), toSnapshot(copy));
    }

    @Override
    @Transactional
    public void createBusinessFromTemplate(Long id, BusinessTemplateCreateBusinessRequestDTO requestDTO) {
        BusinessTemplateEntity template = requireActiveTemplate(id);
        BusinessRegistryCreateRequestDTO createDTO = new BusinessRegistryCreateRequestDTO();
        createDTO.setBusinessCode(normalizeBusinessCode(
                StringFieldUtils.trimToNull(requestDTO.getBusinessCode()) == null
                        ? template.getDefaultBusinessCode()
                        : requestDTO.getBusinessCode()));
        createDTO.setBusinessName(normalizeBusinessName(
                StringFieldUtils.trimToNull(requestDTO.getBusinessName()) == null
                        ? template.getDefaultBusinessName()
                        : requestDTO.getBusinessName()));
        createDTO.setIconMediaId(template.getDefaultIconMediaId());
        createDTO.setDescription(template.getDescription());
        createDTO.setBusinessStatus(template.getDefaultBusinessStatus());
        businessRegistryService.createBusinessRegistry(createDTO);
        log.info("create business from template success templateId={} businessCode={}", template.getId(), createDTO.getBusinessCode());
        recordAudit(ACTION_CREATE_BUSINESS, template.getId(), toSnapshot(template), Map.of("businessCode", createDTO.getBusinessCode()));
    }

    @Override
    @Transactional
    public void reorderBusinessTemplates(BusinessTemplateBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedTemplateIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_ORDERED_IDS);
        }
        List<BusinessTemplateEntity> activeTemplates = listActiveTemplates();
        if (activeTemplates.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }
        Map<Long, BusinessTemplateEntity> entityMap = new HashMap<>();
        for (BusinessTemplateEntity template : activeTemplates) {
            entityMap.put(template.getId(), template);
        }
        if (!entityMap.keySet().equals(new LinkedHashSet<>(requestedOrder))) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }
        List<Map<String, Object>> before = activeTemplates.stream()
                .sorted(templateComparator())
                .map(this::toSnapshot)
                .toList();
        int orderIndex = 0;
        for (Long templateId : requestedOrder) {
            BusinessTemplateEntity entity = entityMap.get(templateId);
            entity.setSortOrder(sortOrderForIndex(orderIndex));
            ConcurrencyHelper.tryUpdate(businessTemplateMapper, entity);
            orderIndex++;
        }
        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();
        log.info("reorder business template success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
    }

    private BusinessTemplateEntity requireActiveTemplate(Long id) {
        BusinessTemplateEntity entity = businessTemplateMapper.selectOne(
                new LambdaQueryWrapper<BusinessTemplateEntity>()
                        .eq(BusinessTemplateEntity::getId, id)
                        .eq(BusinessTemplateEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("business template not found id={}", id);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_NOT_FOUND);
        }
        return entity;
    }

    private List<BusinessTemplateEntity> listActiveTemplates() {
        return businessTemplateMapper.selectList(
                new LambdaQueryWrapper<BusinessTemplateEntity>()
                        .eq(BusinessTemplateEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessTemplateEntity::getSortOrder)
                        .orderByAsc(BusinessTemplateEntity::getId));
    }

    private void applyCreateRequest(BusinessTemplateEntity entity, BusinessTemplateCreateRequestDTO requestDTO) {
        entity.setTemplateCode(normalizeCode(requestDTO.getTemplateCode(), MSG_TEMPLATE_CODE_REQUIRED));
        entity.setTemplateName(normalizeName(requestDTO.getTemplateName(), MSG_TEMPLATE_NAME_REQUIRED));
        entity.setTemplateType(normalizeCode(requestDTO.getTemplateType(), MSG_TEMPLATE_TYPE_REQUIRED));
        entity.setDefaultBusinessCode(normalizeOptionalCode(requestDTO.getDefaultBusinessCode()));
        entity.setDefaultBusinessName(StringFieldUtils.defaultString(requestDTO.getDefaultBusinessName()).trim());
        entity.setDefaultIconMediaId(requestDTO.getDefaultIconMediaId());
        entity.setDefaultBusinessStatus(normalizeStatus(requestDTO.getDefaultBusinessStatus(), STATUS_DRAFT));
        entity.setDescription(StringFieldUtils.defaultString(requestDTO.getDescription()).trim());
        entity.setTemplateConfig(StringFieldUtils.defaultString(requestDTO.getTemplateConfig()).trim());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? nextSortOrder() : requestDTO.getSortOrder());
    }

    private void applyUpdateRequest(BusinessTemplateEntity entity, BusinessTemplateUpdateRequestDTO requestDTO) {
        entity.setTemplateCode(normalizeCode(requestDTO.getTemplateCode(), MSG_TEMPLATE_CODE_REQUIRED));
        entity.setTemplateName(normalizeName(requestDTO.getTemplateName(), MSG_TEMPLATE_NAME_REQUIRED));
        entity.setTemplateType(normalizeCode(requestDTO.getTemplateType(), MSG_TEMPLATE_TYPE_REQUIRED));
        entity.setDefaultBusinessCode(normalizeOptionalCode(requestDTO.getDefaultBusinessCode()));
        entity.setDefaultBusinessName(StringFieldUtils.defaultString(requestDTO.getDefaultBusinessName()).trim());
        entity.setDefaultIconMediaId(requestDTO.getDefaultIconMediaId());
        entity.setDefaultBusinessStatus(normalizeStatus(requestDTO.getDefaultBusinessStatus(), entity.getDefaultBusinessStatus()));
        entity.setDescription(StringFieldUtils.defaultString(requestDTO.getDescription()).trim());
        entity.setTemplateConfig(StringFieldUtils.defaultString(requestDTO.getTemplateConfig()).trim());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? entity.getSortOrder() : requestDTO.getSortOrder());
    }

    private AdminBusinessTemplateVO toAdminVO(BusinessTemplateEntity entity) {
        AdminBusinessTemplateVO vo = new AdminBusinessTemplateVO();
        vo.setId(entity.getId());
        vo.setTemplateCode(StringFieldUtils.defaultString(entity.getTemplateCode()));
        vo.setTemplateName(StringFieldUtils.defaultString(entity.getTemplateName()));
        vo.setTemplateType(StringFieldUtils.defaultString(entity.getTemplateType()));
        vo.setDefaultBusinessCode(StringFieldUtils.defaultString(entity.getDefaultBusinessCode()));
        vo.setDefaultBusinessName(StringFieldUtils.defaultString(entity.getDefaultBusinessName()));
        vo.setDefaultIconMediaId(entity.getDefaultIconMediaId());
        vo.setDefaultBusinessStatus(StringFieldUtils.defaultString(entity.getDefaultBusinessStatus()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setTemplateConfig(StringFieldUtils.defaultString(entity.getTemplateConfig()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        MediaAssetEntity asset = resolveIconAsset(entity.getDefaultIconMediaId(), entity.getId());
        vo.setDefaultIconUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
        return vo;
    }

    private void validateIcon(Long mediaId) {
        if (mediaId != null) {
            mediaAssetService.requireUsableImage(mediaId);
        }
    }

    private void bindIcon(BusinessTemplateEntity entity) {
        mediaAssetService.bindMedia(entity.getDefaultIconMediaId(), TARGET_TYPE, entity.getId(), MEDIA_BIZ_FIELD);
    }

    private MediaAssetEntity resolveIconAsset(Long mediaId, Long templateId) {
        if (mediaId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (BusinessException ex) {
            log.warn("business template icon unavailable templateId={} mediaId={}", templateId, mediaId);
            return null;
        }
    }

    private String nextCopyCode(String templateCode) {
        String base = truncate(templateCode, MAX_CODE_LENGTH - "_COPY".length()) + "_COPY";
        String candidate = base;
        int index = 2;
        while (templateCodeExists(candidate)) {
            String suffix = "_" + index;
            candidate = truncate(base, MAX_CODE_LENGTH - suffix.length()) + suffix;
            index++;
        }
        return candidate;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private boolean templateCodeExists(String templateCode) {
        Long count = businessTemplateMapper.selectCount(
                new LambdaQueryWrapper<BusinessTemplateEntity>()
                        .eq(BusinessTemplateEntity::getDeletedMarker, 0L)
                        .eq(BusinessTemplateEntity::getTemplateCode, templateCode));
        return count != null && count > 0;
    }

    private Map<String, Object> toSnapshot(BusinessTemplateEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("templateCode", entity.getTemplateCode());
        snapshot.put("templateName", entity.getTemplateName());
        snapshot.put("templateType", entity.getTemplateType());
        snapshot.put("defaultBusinessCode", entity.getDefaultBusinessCode());
        snapshot.put("defaultBusinessName", entity.getDefaultBusinessName());
        snapshot.put("defaultIconMediaId", entity.getDefaultIconMediaId());
        snapshot.put("defaultBusinessStatus", entity.getDefaultBusinessStatus());
        snapshot.put("description", entity.getDescription());
        snapshot.put("templateConfig", entity.getTemplateConfig());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private String normalizeBusinessCode(String value) {
        String normalized = normalizeOptionalCode(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_BUSINESS_CODE_REQUIRED);
        }
        return normalized;
    }

    private String normalizeBusinessName(String value) {
        return normalizeName(value, MSG_BUSINESS_NAME_REQUIRED);
    }

    private String normalizeCode(String value, String blankMessage) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, blankMessage);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeOptionalCode(String value) {
        String normalized = StringFieldUtils.trimToNull(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeName(String value, String blankMessage) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, blankMessage);
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
        BusinessTemplateEntity last = businessTemplateMapper.selectOne(
                new LambdaQueryWrapper<BusinessTemplateEntity>()
                        .eq(BusinessTemplateEntity::getDeletedMarker, 0L)
                        .orderByDesc(BusinessTemplateEntity::getSortOrder)
                        .orderByDesc(BusinessTemplateEntity::getId)
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

    private Comparator<BusinessTemplateEntity> templateComparator() {
        return Comparator
                .comparing(BusinessTemplateEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BusinessTemplateEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private int sortGap() {
        return Math.max(1, officialProperties.getCache().getSortGap());
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
