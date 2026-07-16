package com.company.officialwebsite.modules.casecenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.casecenter.converter.CaseConverter;
import com.company.officialwebsite.modules.casecenter.dto.CaseBatchSortDTO;
import com.company.officialwebsite.modules.casecenter.dto.CaseCreateDTO;
import com.company.officialwebsite.modules.casecenter.dto.CaseDeleteDTO;
import com.company.officialwebsite.modules.casecenter.dto.CaseUpdateDTO;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.entity.CaseVersionEntity;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.casecenter.mapper.CaseVersionMapper;
import com.company.officialwebsite.infrastructure.event.EntityChangedEvent;
import com.company.officialwebsite.modules.casecenter.service.CaseService;
import com.company.officialwebsite.modules.casecenter.vo.AdminCaseVO;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseDetailVO;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO;
import com.company.officialwebsite.modules.content.entity.ContentRelationEntity;
import com.company.officialwebsite.modules.content.mapper.ContentRelationMapper;
import com.company.officialwebsite.modules.content.service.ContentReferenceGuard;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.product.converter.ProductConverter;
import com.company.officialwebsite.modules.product.entity.ProductEntity;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import com.company.officialwebsite.modules.product.vo.PortalProductVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CaseServiceImpl：实现标杆案例的 CRUD、排序、媒体生命周期联动、审计和 Portal 缓存逻辑。
 */
@Service
public class CaseServiceImpl implements CaseService {

    private static final Logger log = LoggerFactory.getLogger(CaseServiceImpl.class);

    private static final String CACHE_SEGMENT = "cases";
    private static final String BIZ_MODULE = "CASE_CENTER";
    private static final String TARGET_TYPE = "CASE";
    private static final String ACTION_CREATE = "CREATE_CASE";
    private static final String ACTION_UPDATE = "UPDATE_CASE";
    private static final String ACTION_DELETE = "DELETE_CASE";
    private static final String ACTION_REORDER = "REORDER_CASE";
    private static final String ACTION_STATUS = "UPDATE_CASE_STATUS";
    private static final String MEDIA_BIZ_FIELD = "logo";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";
    private static final String TYPE_PRODUCT = "PRODUCT";
    private static final String REL_PRODUCT_CASE = "PRODUCT_CASE";
    private static final int RECOMMENDATION_LIMIT = 3;
    private static final String MSG_SORT_LIST_EMPTY = "排序列表不能为空";
    private static final String MSG_SORT_LIST_DUPLICATE = "排序列表不能包含重复案例";
    private static final String MSG_NO_SORTABLE_CASE = "暂无可排序的标杆案例";
    private static final String MSG_SORT_LIST_MISMATCH = "排序列表必须完整覆盖当前全部活跃案例";
    private static final String MSG_CASE_DELETED_RETRY = "标杆案例已被删除，请刷新后重试";
    private static final String MSG_CASE_LOGO_MISSING = "案例封面不能为空";
    private static final String MSG_CASE_LOGO_INVALID = "标杆案例的封面/Logo 媒体 ID 不可用";
    private static final String MSG_KEYWORD_EMPTY = "关键词不能为空";
    private static final String MSG_KEYWORD_TOO_LONG = "关键词最长 30 字符";
    private static final String MSG_KEYWORD_DUPLICATE = "关键词不能重复";
    private static final String MSG_REQUIRED_TEXT_EMPTY_SUFFIX = "不能为空";
    private static final String MSG_REQUIRED_TEXT_TOO_LONG_PREFIX = "最长 ";
    private static final String MSG_SORT_VALUE_LIMIT = "排序值已达到上限，请先整理现有案例";

    private final CaseMapper caseMapper;
    private final CaseVersionMapper caseVersionMapper;
    private final CaseConverter caseConverter;
    private final ProductMapper productMapper;
    private final ProductConverter productConverter;
    private final ContentRelationMapper contentRelationMapper;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final ApplicationEventPublisher eventPublisher;
    private final ContentReferenceGuard contentReferenceGuard;
    private final ObjectMapper objectMapper;
    private final int sortGap;

    public CaseServiceImpl(
            CaseMapper caseMapper,
            CaseVersionMapper caseVersionMapper,
            CaseConverter caseConverter,
            ProductMapper productMapper,
            ProductConverter productConverter,
            ContentRelationMapper contentRelationMapper,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport,
            ApplicationEventPublisher eventPublisher,
            ContentReferenceGuard contentReferenceGuard,
            ObjectMapper objectMapper) {
        this.caseMapper = caseMapper;
        this.caseVersionMapper = caseVersionMapper;
        this.caseConverter = caseConverter;
        this.productMapper = productMapper;
        this.productConverter = productConverter;
        this.contentRelationMapper = contentRelationMapper;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.eventPublisher = eventPublisher;
        this.contentReferenceGuard = contentReferenceGuard;
        this.objectMapper = objectMapper;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminCaseVO> getAdminCaseList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);
        Page<CaseEntity> page = caseMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<CaseEntity>()
                        .eq(CaseEntity::getDeletedMarker, 0L)
                        .orderByAsc(CaseEntity::getSortOrder)
                        .orderByAsc(CaseEntity::getId));
        List<AdminCaseVO> list = page.getRecords().stream().map(caseConverter::toAdminVO).toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public List<AdminCaseVO> createCase(CaseCreateDTO createDTO) {
        CaseEntity entity = new CaseEntity();
        entity.setTitle(normalizeRequiredText(createDTO.getTitle(), 128, "项目标题"));
        entity.setLogoMediaId(validateAndResolveLogo(createDTO.getLogoMediaId()));
        entity.setSummary(normalizeRequiredText(createDTO.getSummary(), 512, "成效摘要"));
        entity.setKeywords(normalizeKeywords(createDTO.getKeywords()));
        entity.setVisible(createDTO.getVisible());
        entity.setStatus(normalizeContentStatus(createDTO.getStatus(), STATUS_DRAFT));
        entity.setSortOrder(nextSortOrder());

        try {
            caseMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create case duplicate title title={}", entity.getTitle());
            throw new BusinessException(ErrorCode.CASE_TITLE_DUPLICATE);
        }

        mediaAssetService.bindMedia(entity.getLogoMediaId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        log.info("create case success caseId={} title={} sortOrder={}", entity.getId(), entity.getTitle(), entity.getSortOrder());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
        return listAdminCases();
    }

    @Override
    @Transactional
    public List<AdminCaseVO> updateCase(Long id, CaseUpdateDTO updateDTO) {
        CaseEntity entity = requireActiveCase(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), updateDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        Long oldLogoMediaId = entity.getLogoMediaId();

        entity.setTitle(normalizeRequiredText(updateDTO.getTitle(), 128, "项目标题"));
        entity.setLogoMediaId(validateAndResolveLogo(updateDTO.getLogoMediaId()));
        entity.setSummary(normalizeRequiredText(updateDTO.getSummary(), 512, "成效摘要"));
        entity.setKeywords(normalizeKeywords(updateDTO.getKeywords()));
        entity.setVisible(updateDTO.getVisible());
        entity.setStatus(normalizeContentStatus(updateDTO.getStatus(), defaultExistingStatus(entity.getStatus())));

        try {
            ConcurrencyHelper.tryUpdate(caseMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update case duplicate title caseId={} title={}", entity.getId(), entity.getTitle());
            throw new BusinessException(ErrorCode.CASE_TITLE_DUPLICATE);
        }

        if (!Objects.equals(oldLogoMediaId, entity.getLogoMediaId())) {
            mediaAssetService.bindMedia(entity.getLogoMediaId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        }

        log.info("update case success caseId={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        eventPublisher.publishEvent(EntityChangedEvent.of(this, "casecenter", "Case", String.valueOf(id)));
        invalidatePortalCache();
        return listAdminCases();
    }

    @Override
    @Transactional
    public List<AdminCaseVO> deleteCase(Long id, CaseDeleteDTO deleteDTO) {
        CaseEntity entity = requireActiveCase(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), deleteDTO.getVersion());
        contentReferenceGuard.assertNotReferenced(TARGET_TYPE, entity.getId());
        Map<String, Object> before = toSnapshot(entity);

        int deleted = caseMapper.delete(
                new LambdaUpdateWrapper<CaseEntity>()
                        .eq(CaseEntity::getId, entity.getId())
                        .eq(CaseEntity::getVersion, deleteDTO.getVersion()));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }

        mediaAssetService.bindMedia(null, BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        log.info("delete case success caseId={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        eventPublisher.publishEvent(EntityChangedEvent.of(this, "casecenter", "Case", String.valueOf(entity.getId())));
        invalidatePortalCache();
        return listAdminCases();
    }

    @Override
    @Transactional
    public List<AdminCaseVO> batchSortCases(CaseBatchSortDTO sortDTO) {
        List<Long> orderedIds = sortDTO.getOrderedIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_SORT_LIST_EMPTY);
        }

        Set<Long> deduplicatedIds = new LinkedHashSet<>(orderedIds);
        if (deduplicatedIds.size() != orderedIds.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_SORT_LIST_DUPLICATE);
        }

        List<CaseEntity> activeCases = caseMapper.selectList(
                new LambdaQueryWrapper<CaseEntity>()
                        .eq(CaseEntity::getDeletedMarker, 0L)
                        .orderByAsc(CaseEntity::getId));
        if (activeCases.isEmpty()) {
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND, MSG_NO_SORTABLE_CASE);
        }

        Set<Long> currentIds = new LinkedHashSet<>(activeCases.stream().map(CaseEntity::getId).toList());
        if (!deduplicatedIds.equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_SORT_LIST_MISMATCH);
        }

        Map<Long, CaseEntity> entityMap = new HashMap<>();
        for (CaseEntity entity : activeCases) {
            entityMap.put(entity.getId(), entity);
        }

        List<Map<String, Object>> before = activeCases.stream()
                .sorted(Comparator.comparing(CaseEntity::getSortOrder).thenComparing(CaseEntity::getId))
                .map(this::toSnapshot)
                .toList();

        int orderIndex = 1;
        for (Long caseId : orderedIds) {
            CaseEntity entity = entityMap.get(caseId);
            if (entity == null) {
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_CASE_DELETED_RETRY);
            }
            entity.setSortOrder(orderIndex * sortGap);
            ConcurrencyHelper.tryUpdate(caseMapper, entity);
            orderIndex++;
        }

        List<Map<String, Object>> after = orderedIds.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();

        log.info("batch sort cases success count={}", orderedIds.size());
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
        return listAdminCases();
    }

    @Override
    @Transactional
    public AdminCaseVO updateCaseStatus(Long id, String status, Integer version) {
        CaseEntity entity = requireActiveCase(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        entity.setStatus(normalizeContentStatus(status, null));
        ConcurrencyHelper.tryUpdate(caseMapper, entity);
        log.info("update case status success caseId={} status={}", entity.getId(), entity.getStatus());
        recordAudit(ACTION_STATUS, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
        return caseConverter.toAdminVO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalCaseVO> getPortalCases() {
        String cacheKey = portalCacheSupport.buildKey(CACHE_SEGMENT);
        List<PortalCaseVO> cached = portalCacheSupport.readListCache(cacheKey, PortalCaseVO.class, CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        List<PortalCaseVO> result = caseMapper.selectList(
                new LambdaQueryWrapper<CaseEntity>()
                        .eq(CaseEntity::getDeletedMarker, 0L)
                        .eq(CaseEntity::getVisible, true)
                        .eq(CaseEntity::getStatus, STATUS_PUBLISHED)
                        .orderByAsc(CaseEntity::getSortOrder)
                        .orderByAsc(CaseEntity::getId))
                .stream()
                .map(caseConverter::toPortalVO)
                .toList();
        portalCacheSupport.writeCache(cacheKey, result, portalCacheSupport.isEmptyResult(result), CACHE_SEGMENT);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public PortalCaseDetailVO getPortalCaseDetail(Long id) {
        CaseEntity entity = caseMapper.selectOne(
                new LambdaQueryWrapper<CaseEntity>()
                        .eq(CaseEntity::getId, id)
                        .eq(CaseEntity::getDeletedMarker, 0L)
                        .eq(CaseEntity::getVisible, true)
                        .eq(CaseEntity::getStatus, STATUS_PUBLISHED));
        if (entity == null) {
            log.warn("portal case detail not found caseId={}", id);
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND);
        }
        CaseVersionEntity publishedVersion = caseVersionMapper.selectOne(
                new LambdaQueryWrapper<CaseVersionEntity>()
                        .eq(CaseVersionEntity::getCaseId, id)
                        .orderByDesc(CaseVersionEntity::getVersionNo)
                        .last("LIMIT 1"));
        if (publishedVersion == null || publishedVersion.getSnapshotJson() == null) {
            log.warn("portal case detail has no published snapshot caseId={}", id);
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND);
        }
        try {
            return toPortalDetailFromSnapshot(id, publishedVersion, objectMapper.readTree(publishedVersion.getSnapshotJson()));
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            log.error("portal case detail snapshot is invalid caseId={} versionId={}", id, publishedVersion.getId(), exception);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "案例发布快照格式无效");
        }
    }

    /** Portal 详情仅由已发布快照组装，传统案例实体只承担可见性和状态门禁。 */
    private PortalCaseDetailVO toPortalDetailFromSnapshot(Long caseId, CaseVersionEntity version, JsonNode snapshot) {
        PortalCaseDetailVO detail = new PortalCaseDetailVO();
        detail.setId(caseId);
        detail.setTitle(snapshotText(snapshot, "title"));
        detail.setCustomerName(snapshotText(snapshot, "customerName"));
        detail.setIndustry(snapshotText(snapshot, "industry"));
        detail.setBackground(snapshotText(snapshot, "background"));
        detail.setSolution(snapshotText(snapshot, "solution"));
        detail.setResult(snapshotText(snapshot, "result"));
        detail.setContent(snapshotText(snapshot, "content", "richText", "body"));
        detail.setCoverMediaId(snapshotLong(snapshot, "coverMediaId", "logoMediaId"));
        detail.setCoverUrl(snapshotText(snapshot, "coverUrl", "logoUrl"));
        detail.setImages(snapshotStrings(snapshot, "images", "imageUrls"));
        JsonNode seo = snapshot.path("seo");
        detail.setSeoTitle(snapshotText(seo, "title", "seoTitle"));
        detail.setSeoDescription(snapshotText(seo, "description", "seoDescription"));
        detail.setStatus(STATUS_PUBLISHED);
        detail.setRelatedProducts(listPublishedProductsByIds(snapshotIds(snapshot, "productIds", "relatedProductIds"), RECOMMENDATION_LIMIT)
                .stream().map(productConverter::toPortalVO).toList());
        detail.setRecommendedCases(listPublishedCasesByIds(snapshotIds(snapshot, "recommendedCaseIds", "caseIds"), RECOMMENDATION_LIMIT)
                .stream().map(caseConverter::toPortalVO).toList());
        return detail;
    }

    private String snapshotText(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.isTextual()) {
                return value.asText();
            }
        }
        return "";
    }

    private Long snapshotLong(JsonNode node, String... names) {
        for (String name : names) {
            JsonNode value = node.get(name);
            if (value != null && value.canConvertToLong()) {
                return value.asLong();
            }
        }
        return null;
    }

    private List<Long> snapshotIds(JsonNode snapshot, String... names) {
        for (String name : names) {
            JsonNode ids = snapshot.get(name);
            if (ids != null && ids.isArray()) {
                return java.util.stream.StreamSupport.stream(ids.spliterator(), false)
                        .filter(JsonNode::canConvertToLong)
                        .map(JsonNode::asLong)
                        .toList();
            }
        }
        return List.of();
    }

    private List<String> snapshotStrings(JsonNode snapshot, String... names) {
        for (String name : names) {
            JsonNode values = snapshot.get(name);
            if (values != null && values.isArray()) {
                return java.util.stream.StreamSupport.stream(values.spliterator(), false)
                        .filter(JsonNode::isTextual)
                        .map(JsonNode::asText)
                        .toList();
            }
        }
        return List.of();
    }

    private List<PortalProductVO> listRelatedProducts(Long caseId) {
        List<Long> productIds = listRelatedProductIds(caseId);
        return listPublishedProductsByIds(productIds, RECOMMENDATION_LIMIT).stream()
                .map(productConverter::toPortalVO)
                .toList();
    }

    private List<PortalCaseVO> listRecommendedCases(Long caseId) {
        List<Long> productIds = listRelatedProductIds(caseId);
        if (productIds.isEmpty()) {
            return List.of();
        }

        List<Long> caseIds = contentRelationMapper.selectList(
                        new LambdaQueryWrapper<ContentRelationEntity>()
                                .eq(ContentRelationEntity::getDeletedMarker, 0L)
                                .eq(ContentRelationEntity::getSourceType, TYPE_PRODUCT)
                                .in(ContentRelationEntity::getSourceId, productIds)
                                .eq(ContentRelationEntity::getTargetType, TARGET_TYPE)
                                .eq(ContentRelationEntity::getRelationType, REL_PRODUCT_CASE)
                                .ne(ContentRelationEntity::getTargetId, caseId)
                                .orderByAsc(ContentRelationEntity::getId))
                .stream()
                .map(ContentRelationEntity::getTargetId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return listPublishedCasesByIds(caseIds, RECOMMENDATION_LIMIT).stream()
                .map(caseConverter::toPortalVO)
                .toList();
    }

    private List<Long> listRelatedProductIds(Long caseId) {
        return contentRelationMapper.selectList(
                        new LambdaQueryWrapper<ContentRelationEntity>()
                                .eq(ContentRelationEntity::getDeletedMarker, 0L)
                                .eq(ContentRelationEntity::getSourceType, TYPE_PRODUCT)
                                .eq(ContentRelationEntity::getTargetType, TARGET_TYPE)
                                .eq(ContentRelationEntity::getTargetId, caseId)
                                .eq(ContentRelationEntity::getRelationType, REL_PRODUCT_CASE)
                                .orderByAsc(ContentRelationEntity::getId))
                .stream()
                .map(ContentRelationEntity::getSourceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private List<ProductEntity> listPublishedProductsByIds(List<Long> productIds, int limit) {
        if (productIds.isEmpty()) {
            return List.of();
        }
        Map<Long, ProductEntity> productMap = new HashMap<>();
        productMapper.selectList(
                        new LambdaQueryWrapper<ProductEntity>()
                                .in(ProductEntity::getId, productIds)
                                .eq(ProductEntity::getDeletedMarker, 0L)
                                .eq(ProductEntity::getVisible, 1)
                                .eq(ProductEntity::getStatus, STATUS_PUBLISHED))
                .forEach(product -> productMap.put(product.getId(), product));
        return productIds.stream()
                .map(productMap::get)
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
    }

    private List<CaseEntity> listPublishedCasesByIds(List<Long> caseIds, int limit) {
        if (caseIds.isEmpty()) {
            return List.of();
        }
        Map<Long, CaseEntity> caseMap = new HashMap<>();
        caseMapper.selectList(
                        new LambdaQueryWrapper<CaseEntity>()
                                .in(CaseEntity::getId, caseIds)
                                .eq(CaseEntity::getDeletedMarker, 0L)
                                .eq(CaseEntity::getVisible, true)
                                .eq(CaseEntity::getStatus, STATUS_PUBLISHED))
                .forEach(caseEntity -> caseMap.put(caseEntity.getId(), caseEntity));
        return caseIds.stream()
                .map(caseMap::get)
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
    }

    private CaseEntity requireActiveCase(Long id) {
        CaseEntity entity = caseMapper.selectOne(
                new LambdaQueryWrapper<CaseEntity>()
                        .eq(CaseEntity::getId, id)
                        .eq(CaseEntity::getDeletedMarker, 0L));
        if (entity == null) {
            log.warn("case not found caseId={}", id);
            throw new BusinessException(ErrorCode.CASE_NOT_FOUND);
        }
        return entity;
    }

    private List<AdminCaseVO> listAdminCases() {
        return caseMapper.selectList(
                        new LambdaQueryWrapper<CaseEntity>()
                                .eq(CaseEntity::getDeletedMarker, 0L)
                                .orderByAsc(CaseEntity::getSortOrder)
                                .orderByAsc(CaseEntity::getId))
                .stream()
                .map(caseConverter::toAdminVO)
                .toList();
    }

    private Long validateAndResolveLogo(Long logoMediaId) {
        if (logoMediaId == null) {
            log.warn("case logo missing");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_CASE_LOGO_MISSING);
        }
        try {
            mediaAssetService.requireUsableImage(logoMediaId);
            return logoMediaId;
        } catch (BusinessException ex) {
            log.warn("case logo unavailable logoMediaId={}", logoMediaId, ex);
            throw new BusinessException(ErrorCode.CASE_LOGO_INVALID, MSG_CASE_LOGO_INVALID, ex);
        }
    }

    private List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return List.of();
        }
        if (keywords.size() > 10) {
            log.warn("case keywords validation failed reason=too_many count={}", keywords.size());
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "核心关键词标签最多 10 个");
        }
        List<String> normalized = new ArrayList<>();
        for (String keyword : keywords) {
            String normalizedKeyword = StringFieldUtils.trimToNull(keyword);
            if (normalizedKeyword == null) {
                log.warn("case keywords validation failed reason=blank");
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_KEYWORD_EMPTY);
            }
            if (normalizedKeyword.length() > 30) {
                log.warn("case keywords validation failed reason=too_long length={}", normalizedKeyword.length());
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_KEYWORD_TOO_LONG);
            }
            if (normalized.contains(normalizedKeyword)) {
                log.warn("case keywords validation failed reason=duplicate");
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_KEYWORD_DUPLICATE);
            }
            normalized.add(normalizedKeyword);
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, int maxLength, String fieldName) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            log.warn("case text validation failed field={} reason=blank", fieldName);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, fieldName + MSG_REQUIRED_TEXT_EMPTY_SUFFIX);
        }
        if (normalized.length() > maxLength) {
            log.warn("case text validation failed field={} reason=too_long maxLength={}", fieldName, maxLength);
            throw new BusinessException(
                    ErrorCode.COMMON_PARAM_INVALID,
                    fieldName + MSG_REQUIRED_TEXT_TOO_LONG_PREFIX + maxLength + " 字符");
        }
        return normalized;
    }

    private int nextSortOrder() {
        CaseEntity last = caseMapper.selectOne(
                new LambdaQueryWrapper<CaseEntity>()
                        .eq(CaseEntity::getDeletedMarker, 0L)
                        .orderByDesc(CaseEntity::getSortOrder)
                        .orderByDesc(CaseEntity::getId)
                        .last("limit 1"));
        int current = (last == null || last.getSortOrder() == null) ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            log.warn("case sort order overflow current={} gap={}", current, sortGap);
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_SORT_VALUE_LIMIT);
        }
        return current + sortGap;
    }

    private Map<String, Object> toSnapshot(CaseEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("title", entity.getTitle());
        snapshot.put("logoMediaId", entity.getLogoMediaId());
        snapshot.put("summary", entity.getSummary());
        snapshot.put("keywords", entity.getKeywords());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("status", entity.getStatus());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void invalidatePortalCache() {
        portalCacheSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private String normalizeContentStatus(String status, String defaultStatus) {
        String normalized = status == null || status.isBlank() ? defaultStatus : status.trim().toUpperCase();
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "内容状态不能为空");
        }
        if (!Set.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_OFFLINE).contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "内容状态只能是 DRAFT、PUBLISHED 或 OFFLINE");
        }
        return normalized;
    }

    private String defaultExistingStatus(String status) {
        return status == null || status.isBlank() ? STATUS_DRAFT : status.trim().toUpperCase();
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
