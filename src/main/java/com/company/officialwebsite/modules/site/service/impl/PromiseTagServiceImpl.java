package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.site.dto.PromiseTagBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.PromiseTagCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.PromiseTagUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.PromiseTagEntity;
import com.company.officialwebsite.modules.site.mapper.PromiseTagMapper;
import com.company.officialwebsite.modules.site.service.PromiseModuleConstants;
import com.company.officialwebsite.modules.site.service.PromiseTagService;
import com.company.officialwebsite.modules.site.vo.AdminPromiseTagVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PromiseTagServiceImpl：实现承诺标签的后台维护、审计和缓存失效逻辑。
 */
@Service
public class PromiseTagServiceImpl implements PromiseTagService {

    private static final Logger log = LoggerFactory.getLogger(PromiseTagServiceImpl.class);

    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "PROMISE_TAG";
    private static final String ACTION_CREATE = "CREATE_PROMISE_TAG";
    private static final String ACTION_UPDATE = "UPDATE_PROMISE_TAG";
    private static final String ACTION_DELETE = "DELETE_PROMISE_TAG";
    private static final String ACTION_REORDER = "REORDER_PROMISE_TAG";
    private static final String MSG_EMPTY_ORDERED_IDS = "排序标签列表不能为空";
    private static final String MSG_INVALID_ORDERED_IDS = "排序列表包含不存在或已删除的标签";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "排序列表必须完整覆盖全部活跃标签";
    private static final String MSG_TAG_TEXT_REQUIRED = "标签文本不能为空";
    private static final String MSG_SORT_ORDER_LIMIT = "承诺标签排序值已达到上限";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "排序值超出允许范围";
    private static final String MSG_ORDERED_ID_REQUIRED = "排序标签 ID 不能为空";

    private final PromiseTagMapper promiseTagMapper;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final OfficialProperties officialProperties;

    public PromiseTagServiceImpl(
            PromiseTagMapper promiseTagMapper,
            AuditLogService auditLogService,
            PortalCacheSupport portalCacheSupport,
            OfficialProperties officialProperties) {
        this.promiseTagMapper = promiseTagMapper;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminPromiseTagVO> getAdminPromiseTagList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);
        Page<PromiseTagEntity> page = promiseTagMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<PromiseTagEntity>()
                        .eq(PromiseTagEntity::getDeletedMarker, 0L)
                        .orderByAsc(PromiseTagEntity::getSortOrder)
                        .orderByAsc(PromiseTagEntity::getId));
        List<AdminPromiseTagVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createPromiseTag(PromiseTagCreateRequestDTO requestDTO) {
        PromiseTagEntity entity = new PromiseTagEntity();
        entity.setTagText(normalizeTagText(requestDTO.getTagText()));
        entity.setDescription(normalizeDescription(requestDTO.getDescription()));
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(nextSortOrder());
        try {
            promiseTagMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create promise tag duplicate tagText={}", entity.getTagText(), ex);
            throw new BusinessException(ErrorCode.SITE_PROMISE_TAG_TEXT_DUPLICATE);
        }
        log.info("create promise tag success id={} tagText={}", entity.getId(), entity.getTagText());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void updatePromiseTag(Long id, PromiseTagUpdateRequestDTO requestDTO) {
        PromiseTagEntity entity = requireActiveTag(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        entity.setTagText(normalizeTagText(requestDTO.getTagText()));
        entity.setDescription(normalizeDescription(requestDTO.getDescription()));
        entity.setVisible(requestDTO.getVisible() == null ? Boolean.TRUE.equals(entity.getVisible()) : requestDTO.getVisible());
        try {
            ConcurrencyHelper.tryUpdate(promiseTagMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update promise tag duplicate id={} tagText={}", entity.getId(), entity.getTagText(), ex);
            throw new BusinessException(ErrorCode.SITE_PROMISE_TAG_TEXT_DUPLICATE);
        }
        log.info("update promise tag success id={} tagText={}", entity.getId(), entity.getTagText());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void deletePromiseTag(Long id, Integer version) {
        PromiseTagEntity entity = requireActiveTag(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = promiseTagMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete promise tag success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void reorderPromiseTags(PromiseTagBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedPromiseTagIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_ORDERED_IDS);
        }

        List<PromiseTagEntity> targetEntities = promiseTagMapper.selectList(
                new LambdaQueryWrapper<PromiseTagEntity>()
                        .eq(PromiseTagEntity::getDeletedMarker, 0L)
                        .in(PromiseTagEntity::getId, requestedOrder));
        if (targetEntities.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }

        List<PromiseTagEntity> activeTags = listActiveTags();
        Set<Long> currentIds = new LinkedHashSet<>(activeTags.stream()
                .map(PromiseTagEntity::getId)
                .toList());
        if (!new LinkedHashSet<>(requestedOrder).equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }

        Map<Long, PromiseTagEntity> entityMap = new HashMap<>();
        for (PromiseTagEntity tag : activeTags) {
            entityMap.put(tag.getId(), tag);
        }

        List<Map<String, Object>> before = activeTags.stream()
                .sorted(tagComparator())
                .map(this::toSnapshot)
                .toList();

        for (int index = 0; index < requestedOrder.size(); index++) {
            PromiseTagEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            ConcurrencyHelper.tryUpdate(promiseTagMapper, entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();

        log.info("reorder promise tags success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
    }

    private PromiseTagEntity requireActiveTag(Long id) {
        PromiseTagEntity entity = promiseTagMapper.selectOne(
                new LambdaQueryWrapper<PromiseTagEntity>()
                        .eq(PromiseTagEntity::getId, id)
                        .eq(PromiseTagEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("promise tag not found id={}", id);
            throw new BusinessException(ErrorCode.SITE_PROMISE_TAG_NOT_FOUND);
        }
        return entity;
    }

    private List<PromiseTagEntity> listActiveTags() {
        return promiseTagMapper.selectList(
                new LambdaQueryWrapper<PromiseTagEntity>()
                        .eq(PromiseTagEntity::getDeletedMarker, 0L)
                        .orderByAsc(PromiseTagEntity::getSortOrder)
                        .orderByAsc(PromiseTagEntity::getId));
    }

    private AdminPromiseTagVO toAdminVO(PromiseTagEntity entity) {
        AdminPromiseTagVO vo = new AdminPromiseTagVO();
        vo.setId(entity.getId());
        vo.setTagText(StringFieldUtils.defaultString(entity.getTagText()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Map<String, Object> toSnapshot(PromiseTagEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("tagText", entity.getTagText());
        snapshot.put("description", entity.getDescription());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private String normalizeTagText(String tagText) {
        String normalized = StringFieldUtils.trimToNull(tagText);
        if (normalized == null) {
            log.warn("promise tag text blank");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_TAG_TEXT_REQUIRED);
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        return StringFieldUtils.trimToNull(description);
    }

    private int nextSortOrder() {
        PromiseTagEntity last = promiseTagMapper.selectOne(
                new LambdaQueryWrapper<PromiseTagEntity>()
                        .eq(PromiseTagEntity::getDeletedMarker, 0L)
                        .orderByDesc(PromiseTagEntity::getSortOrder)
                        .orderByDesc(PromiseTagEntity::getId)
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

    private Comparator<PromiseTagEntity> tagComparator() {
        return Comparator
                .comparing(PromiseTagEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(PromiseTagEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private int sortGap() {
        return Math.max(1, officialProperties.getCache().getSortGap());
    }

    private void invalidatePortalCache() {
        portalCacheSupport.invalidatePortalKey(PromiseModuleConstants.CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
