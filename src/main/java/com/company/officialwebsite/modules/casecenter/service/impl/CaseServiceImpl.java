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
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.casecenter.service.CaseService;
import com.company.officialwebsite.modules.casecenter.vo.AdminCaseVO;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.system.service.AuditLogService;
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
    private static final String MEDIA_BIZ_FIELD = "logo";
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
    private final CaseConverter caseConverter;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final int sortGap;

    public CaseServiceImpl(
            CaseMapper caseMapper,
            CaseConverter caseConverter,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport) {
        this.caseMapper = caseMapper;
        this.caseConverter = caseConverter;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
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
        invalidatePortalCache();
        return listAdminCases();
    }

    @Override
    @Transactional
    public List<AdminCaseVO> deleteCase(Long id, CaseDeleteDTO deleteDTO) {
        CaseEntity entity = requireActiveCase(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), deleteDTO.getVersion());
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
                        .orderByAsc(CaseEntity::getSortOrder)
                        .orderByAsc(CaseEntity::getId))
                .stream()
                .map(caseConverter::toPortalVO)
                .toList();
        portalCacheSupport.writeCache(cacheKey, result, portalCacheSupport.isEmptyResult(result), CACHE_SEGMENT);
        return result;
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
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void invalidatePortalCache() {
        portalCacheSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
