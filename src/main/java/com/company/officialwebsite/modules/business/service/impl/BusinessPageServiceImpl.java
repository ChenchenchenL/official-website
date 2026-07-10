package com.company.officialwebsite.modules.business.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.business.dto.BusinessPageBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageUpdateRequestDTO;
import com.company.officialwebsite.modules.business.entity.BusinessPageEntity;
import com.company.officialwebsite.modules.business.entity.BusinessRegistryEntity;
import com.company.officialwebsite.modules.business.entity.BusinessTemplateEntity;
import com.company.officialwebsite.modules.business.mapper.BusinessPageMapper;
import com.company.officialwebsite.modules.business.mapper.BusinessRegistryMapper;
import com.company.officialwebsite.modules.business.mapper.BusinessTemplateMapper;
import com.company.officialwebsite.modules.business.service.BusinessPageService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessPageVO;
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
public class BusinessPageServiceImpl implements BusinessPageService {

    private static final Logger log = LoggerFactory.getLogger(BusinessPageServiceImpl.class);

    private static final String BIZ_MODULE = "BUSINESS";
    private static final String TARGET_TYPE = "BUSINESS_PAGE";
    private static final String ACTION_CREATE = "CREATE_BUSINESS_PAGE";
    private static final String ACTION_UPDATE = "UPDATE_BUSINESS_PAGE";
    private static final String ACTION_DELETE = "DELETE_BUSINESS_PAGE";
    private static final String ACTION_REORDER = "REORDER_BUSINESS_PAGE";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_ONLINE = "ONLINE";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String MSG_NOT_FOUND = "Business page does not exist or has been deleted";
    private static final String MSG_DUPLICATE = "Business page code or route path already exists";
    private static final String MSG_BUSINESS_NOT_FOUND = "Business does not exist or has been deleted";
    private static final String MSG_TEMPLATE_NOT_FOUND = "Business template does not exist or has been deleted";
    private static final String MSG_PAGE_CODE_REQUIRED = "Page code cannot be empty";
    private static final String MSG_PAGE_NAME_REQUIRED = "Page name cannot be empty";
    private static final String MSG_ROUTE_REQUIRED = "Route path cannot be empty";
    private static final String MSG_ROUTE_INVALID = "Route path must start with /";
    private static final String MSG_STATUS_INVALID = "Page status must be DRAFT, ONLINE, or OFFLINE";
    private static final String MSG_EMPTY_ORDERED_IDS = "Ordered page id list cannot be empty";
    private static final String MSG_INVALID_ORDERED_IDS = "Ordered page id list contains invalid pages";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "Ordered page id list must cover all active pages";
    private static final String MSG_ORDERED_ID_REQUIRED = "Ordered page id cannot be empty";
    private static final String MSG_SORT_ORDER_LIMIT = "Page sort order has reached the limit";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "Page sort order is out of range";

    private final BusinessPageMapper businessPageMapper;
    private final BusinessRegistryMapper businessRegistryMapper;
    private final BusinessTemplateMapper businessTemplateMapper;
    private final AuditLogService auditLogService;
    private final OfficialProperties officialProperties;

    public BusinessPageServiceImpl(
            BusinessPageMapper businessPageMapper,
            BusinessRegistryMapper businessRegistryMapper,
            BusinessTemplateMapper businessTemplateMapper,
            AuditLogService auditLogService,
            OfficialProperties officialProperties) {
        this.businessPageMapper = businessPageMapper;
        this.businessRegistryMapper = businessRegistryMapper;
        this.businessTemplateMapper = businessTemplateMapper;
        this.auditLogService = auditLogService;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminBusinessPageVO> getAdminBusinessPageList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        Page<BusinessPageEntity> page = businessPageMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<BusinessPageEntity>()
                        .eq(BusinessPageEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessPageEntity::getSortOrder)
                        .orderByAsc(BusinessPageEntity::getId));
        List<AdminBusinessPageVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createBusinessPage(BusinessPageCreateRequestDTO requestDTO) {
        BusinessPageEntity entity = new BusinessPageEntity();
        applyCreateRequest(entity, requestDTO);
        try {
            businessPageMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create business page duplicate pageCode={} routePath={}", entity.getPageCode(), entity.getRoutePath(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }
        log.info("create business page success id={} pageCode={}", entity.getId(), entity.getPageCode());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void updateBusinessPage(Long id, BusinessPageUpdateRequestDTO requestDTO) {
        BusinessPageEntity entity = requireActivePage(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        applyUpdateRequest(entity, requestDTO);
        try {
            ConcurrencyHelper.tryUpdate(businessPageMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update business page duplicate id={} pageCode={}", entity.getId(), entity.getPageCode(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }
        log.info("update business page success id={} pageCode={}", entity.getId(), entity.getPageCode());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void deleteBusinessPage(Long id, Integer version) {
        BusinessPageEntity entity = requireActivePage(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = businessPageMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete business page success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    @Override
    @Transactional
    public void reorderBusinessPages(BusinessPageBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedPageIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_ORDERED_IDS);
        }
        List<BusinessPageEntity> activePages = listActivePages();
        if (activePages.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }
        Map<Long, BusinessPageEntity> entityMap = new HashMap<>();
        for (BusinessPageEntity page : activePages) {
            entityMap.put(page.getId(), page);
        }
        if (!entityMap.keySet().equals(new LinkedHashSet<>(requestedOrder))) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }
        List<Map<String, Object>> before = activePages.stream()
                .sorted(pageComparator())
                .map(this::toSnapshot)
                .toList();
        for (int index = 0; index < requestedOrder.size(); index++) {
            BusinessPageEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            ConcurrencyHelper.tryUpdate(businessPageMapper, entity);
        }
        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();
        log.info("reorder business page success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
    }

    private BusinessPageEntity requireActivePage(Long id) {
        BusinessPageEntity entity = businessPageMapper.selectOne(
                new LambdaQueryWrapper<BusinessPageEntity>()
                        .eq(BusinessPageEntity::getId, id)
                        .eq(BusinessPageEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_NOT_FOUND);
        }
        return entity;
    }

    private List<BusinessPageEntity> listActivePages() {
        return businessPageMapper.selectList(
                new LambdaQueryWrapper<BusinessPageEntity>()
                        .eq(BusinessPageEntity::getDeletedMarker, 0L)
                        .orderByAsc(BusinessPageEntity::getSortOrder)
                        .orderByAsc(BusinessPageEntity::getId));
    }

    private void applyCreateRequest(BusinessPageEntity entity, BusinessPageCreateRequestDTO requestDTO) {
        requireActiveBusiness(requestDTO.getBusinessId());
        if (requestDTO.getTemplateId() != null) {
            requireActiveTemplate(requestDTO.getTemplateId());
        }
        entity.setBusinessId(requestDTO.getBusinessId());
        entity.setTemplateId(requestDTO.getTemplateId());
        entity.setPageCode(normalizeCode(requestDTO.getPageCode(), MSG_PAGE_CODE_REQUIRED));
        entity.setPageName(normalizeName(requestDTO.getPageName(), MSG_PAGE_NAME_REQUIRED));
        entity.setRoutePath(normalizeRoutePath(requestDTO.getRoutePath()));
        entity.setPageStatus(normalizeStatus(requestDTO.getPageStatus(), STATUS_DRAFT));
        entity.setPageConfig(StringFieldUtils.defaultString(requestDTO.getPageConfig()).trim());
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? nextSortOrder() : requestDTO.getSortOrder());
    }

    private void applyUpdateRequest(BusinessPageEntity entity, BusinessPageUpdateRequestDTO requestDTO) {
        requireActiveBusiness(requestDTO.getBusinessId());
        if (requestDTO.getTemplateId() != null) {
            requireActiveTemplate(requestDTO.getTemplateId());
        }
        entity.setBusinessId(requestDTO.getBusinessId());
        entity.setTemplateId(requestDTO.getTemplateId());
        entity.setPageCode(normalizeCode(requestDTO.getPageCode(), MSG_PAGE_CODE_REQUIRED));
        entity.setPageName(normalizeName(requestDTO.getPageName(), MSG_PAGE_NAME_REQUIRED));
        entity.setRoutePath(normalizeRoutePath(requestDTO.getRoutePath()));
        entity.setPageStatus(normalizeStatus(requestDTO.getPageStatus(), entity.getPageStatus()));
        entity.setPageConfig(StringFieldUtils.defaultString(requestDTO.getPageConfig()).trim());
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? entity.getSortOrder() : requestDTO.getSortOrder());
    }

    private BusinessRegistryEntity requireActiveBusiness(Long id) {
        BusinessRegistryEntity entity = businessRegistryMapper.selectOne(
                new LambdaQueryWrapper<BusinessRegistryEntity>()
                        .eq(BusinessRegistryEntity::getId, id)
                        .eq(BusinessRegistryEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_BUSINESS_NOT_FOUND);
        }
        return entity;
    }

    private BusinessTemplateEntity requireActiveTemplate(Long id) {
        BusinessTemplateEntity entity = businessTemplateMapper.selectOne(
                new LambdaQueryWrapper<BusinessTemplateEntity>()
                        .eq(BusinessTemplateEntity::getId, id)
                        .eq(BusinessTemplateEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_TEMPLATE_NOT_FOUND);
        }
        return entity;
    }

    private AdminBusinessPageVO toAdminVO(BusinessPageEntity entity) {
        AdminBusinessPageVO vo = new AdminBusinessPageVO();
        vo.setId(entity.getId());
        vo.setBusinessId(entity.getBusinessId());
        vo.setTemplateId(entity.getTemplateId());
        vo.setPageCode(StringFieldUtils.defaultString(entity.getPageCode()));
        vo.setPageName(StringFieldUtils.defaultString(entity.getPageName()));
        vo.setRoutePath(StringFieldUtils.defaultString(entity.getRoutePath()));
        vo.setPageStatus(StringFieldUtils.defaultString(entity.getPageStatus()));
        vo.setPageConfig(StringFieldUtils.defaultString(entity.getPageConfig()));
        vo.setVisible(entity.getVisible() == null || entity.getVisible());
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        BusinessRegistryEntity business = entity.getBusinessId() == null ? null : businessRegistryMapper.selectById(entity.getBusinessId());
        BusinessTemplateEntity template = entity.getTemplateId() == null ? null : businessTemplateMapper.selectById(entity.getTemplateId());
        vo.setBusinessName(business == null ? "" : StringFieldUtils.defaultString(business.getBusinessName()));
        vo.setTemplateName(template == null ? "" : StringFieldUtils.defaultString(template.getTemplateName()));
        return vo;
    }

    private Map<String, Object> toSnapshot(BusinessPageEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("businessId", entity.getBusinessId());
        snapshot.put("templateId", entity.getTemplateId());
        snapshot.put("pageCode", entity.getPageCode());
        snapshot.put("pageName", entity.getPageName());
        snapshot.put("routePath", entity.getRoutePath());
        snapshot.put("pageStatus", entity.getPageStatus());
        snapshot.put("pageConfig", entity.getPageConfig());
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

    private String normalizeRoutePath(String value) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_ROUTE_REQUIRED);
        }
        if (!normalized.startsWith("/")) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_ROUTE_INVALID);
        }
        return normalized;
    }

    private String normalizeStatus(String status, String defaultStatus) {
        String normalized = status == null || status.isBlank() ? defaultStatus : status.trim().toUpperCase(Locale.ROOT);
        if (normalized == null || !Set.of(STATUS_DRAFT, STATUS_ONLINE, STATUS_OFFLINE).contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_STATUS_INVALID);
        }
        return normalized;
    }

    private int nextSortOrder() {
        BusinessPageEntity last = businessPageMapper.selectOne(
                new LambdaQueryWrapper<BusinessPageEntity>()
                        .eq(BusinessPageEntity::getDeletedMarker, 0L)
                        .orderByDesc(BusinessPageEntity::getSortOrder)
                        .orderByDesc(BusinessPageEntity::getId)
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

    private Comparator<BusinessPageEntity> pageComparator() {
        return Comparator
                .comparing(BusinessPageEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(BusinessPageEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private int sortGap() {
        return Math.max(1, officialProperties.getCache().getSortGap());
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
