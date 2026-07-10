package com.company.officialwebsite.modules.lead.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.lead.dto.CooperationDirectionTagBatchSortRequestDTO;
import com.company.officialwebsite.modules.lead.dto.CooperationDirectionTagCreateRequestDTO;
import com.company.officialwebsite.modules.lead.dto.CooperationDirectionTagUpdateRequestDTO;
import com.company.officialwebsite.modules.lead.entity.CooperationDirectionTagEntity;
import com.company.officialwebsite.modules.lead.mapper.CooperationDirectionTagMapper;
import com.company.officialwebsite.modules.lead.service.CooperationDirectionTagModuleConstants;
import com.company.officialwebsite.modules.lead.service.CooperationDirectionTagService;
import com.company.officialwebsite.modules.lead.vo.AdminCooperationDirectionTagVO;
import com.company.officialwebsite.modules.lead.vo.PortalCooperationDirectionTagVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import com.company.officialwebsite.infrastructure.event.EntityChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CooperationDirectionTagServiceImpl：实现合作方向标签的后台维护、前台缓存读取、审计和缓存失效逻辑。
 */
@Service
public class CooperationDirectionTagServiceImpl implements CooperationDirectionTagService {

    private static final Logger log = LoggerFactory.getLogger(CooperationDirectionTagServiceImpl.class);

    private static final String BIZ_MODULE = "LEAD";
    private static final String TARGET_TYPE = "COOPERATION_DIRECTION_TAG";
    private static final String ACTION_CREATE = "CREATE_COOPERATION_DIRECTION_TAG";
    private static final String ACTION_UPDATE = "UPDATE_COOPERATION_DIRECTION_TAG";
    private static final String ACTION_DELETE = "DELETE_COOPERATION_DIRECTION_TAG";
    private static final String ACTION_REORDER = "REORDER_COOPERATION_DIRECTION_TAG";
    private static final String MSG_INVALID_ORDERED_IDS = "排序列表包含不存在或已删除的标签";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "排序列表必须完整覆盖全部活跃标签";
    private static final String MSG_TAG_TEXT_REQUIRED = "标签文本不能为空";
    private static final String MSG_ORDERED_ID_REQUIRED = "排序标签 ID 不能为空";
    private static final String MSG_DUPLICATE_ORDERED_IDS = "排序列表包含重复标签 ID";

    private final CooperationDirectionTagMapper cooperationDirectionTagMapper;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final ApplicationEventPublisher eventPublisher;
    private final int sortGap;

    public CooperationDirectionTagServiceImpl(
            CooperationDirectionTagMapper cooperationDirectionTagMapper,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport,
            ApplicationEventPublisher eventPublisher) {
        this.cooperationDirectionTagMapper = cooperationDirectionTagMapper;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.eventPublisher = eventPublisher;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminCooperationDirectionTagVO> getAdminCooperationDirectionTagList() {
        return listActiveTags().stream()
                .map(this::toAdminVO)
                .toList();
    }

    @Override
    @Transactional
    public void createCooperationDirectionTag(CooperationDirectionTagCreateRequestDTO requestDTO) {
        CooperationDirectionTagEntity entity = new CooperationDirectionTagEntity();
        entity.setTagText(normalizeTagText(requestDTO.getTagText()));
        entity.setSortOrder(nextSortOrder());
        try {
            cooperationDirectionTagMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create cooperation direction tag duplicate tagText={}", entity.getTagText(), ex);
            throw new BusinessException(ErrorCode.LEAD_COOPERATION_DIRECTION_TAG_TEXT_DUPLICATE);
        }
        log.info("create cooperation direction tag success id={} tagText={}", entity.getId(), entity.getTagText());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
        eventPublisher.publishEvent(EntityChangedEvent.of(this, "lead", "CooperationDirectionTag", String.valueOf(entity.getId())));
    }

    @Override
    @Transactional
    public void updateCooperationDirectionTag(Long id, CooperationDirectionTagUpdateRequestDTO requestDTO) {
        CooperationDirectionTagEntity entity = requireActiveTag(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        entity.setTagText(normalizeTagText(requestDTO.getTagText()));
        try {
            ConcurrencyHelper.tryUpdate(cooperationDirectionTagMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update cooperation direction tag duplicate id={} tagText={}", entity.getId(), entity.getTagText(), ex);
            throw new BusinessException(ErrorCode.LEAD_COOPERATION_DIRECTION_TAG_TEXT_DUPLICATE);
        }
        log.info("update cooperation direction tag success id={} tagText={}", entity.getId(), entity.getTagText());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
        eventPublisher.publishEvent(EntityChangedEvent.of(this, "lead", "CooperationDirectionTag", String.valueOf(entity.getId())));
    }

    @Override
    @Transactional
    public void deleteCooperationDirectionTag(Long id, Integer version) {
        CooperationDirectionTagEntity entity = requireActiveTag(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = cooperationDirectionTagMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete cooperation direction tag success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
        eventPublisher.publishEvent(EntityChangedEvent.of(this, "lead", "CooperationDirectionTag", String.valueOf(entity.getId())));
    }

    @Override
    @Transactional
    public void reorderCooperationDirectionTags(CooperationDirectionTagBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedCooperationDirectionTagIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_ORDERED_ID_REQUIRED);
        }

        List<CooperationDirectionTagEntity> targetEntities = cooperationDirectionTagMapper.selectList(
                new LambdaQueryWrapper<CooperationDirectionTagEntity>()
                        .eq(CooperationDirectionTagEntity::getDeletedMarker, 0L)
                        .in(CooperationDirectionTagEntity::getId, requestedOrder));
        if (targetEntities.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }

        List<CooperationDirectionTagEntity> activeTags = listActiveTags();
        if (activeTags.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_ORDERED_ID_REQUIRED);
        }
        Set<Long> currentIds = new LinkedHashSet<>(activeTags.stream()
                .map(CooperationDirectionTagEntity::getId)
                .toList());
        if (!new LinkedHashSet<>(requestedOrder).equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }

        Map<Long, CooperationDirectionTagEntity> entityMap = new HashMap<>();
        for (CooperationDirectionTagEntity tag : activeTags) {
            entityMap.put(tag.getId(), tag);
        }

        List<Map<String, Object>> before = activeTags.stream()
                .sorted(tagComparator())
                .map(this::toSnapshot)
                .toList();

        for (int index = 0; index < requestedOrder.size(); index++) {
            CooperationDirectionTagEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            ConcurrencyHelper.tryUpdate(cooperationDirectionTagMapper, entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();

        log.info("reorder cooperation direction tags success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
        eventPublisher.publishEvent(EntityChangedEvent.of(this, "lead", "CooperationDirectionTag", "default"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalCooperationDirectionTagVO> getPortalCooperationDirectionTagList() {
        String cacheKey = portalCacheSupport.buildKey(CooperationDirectionTagModuleConstants.CACHE_SEGMENT);
        List<PortalCooperationDirectionTagVO> cached = portalCacheSupport.readListCache(
                cacheKey, PortalCooperationDirectionTagVO.class, CooperationDirectionTagModuleConstants.CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        List<PortalCooperationDirectionTagVO> result = listActiveTags().stream()
                .map(this::toPortalVO)
                .toList();
        portalCacheSupport.writeCache(cacheKey, result, portalCacheSupport.isEmptyResult(result),
                CooperationDirectionTagModuleConstants.CACHE_SEGMENT);
        return result;
    }

    private CooperationDirectionTagEntity requireActiveTag(Long id) {
        CooperationDirectionTagEntity entity = cooperationDirectionTagMapper.selectOne(
                new LambdaQueryWrapper<CooperationDirectionTagEntity>()
                        .eq(CooperationDirectionTagEntity::getId, id)
                        .eq(CooperationDirectionTagEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("cooperation direction tag not found id={}", id);
            throw new BusinessException(ErrorCode.LEAD_COOPERATION_DIRECTION_TAG_NOT_FOUND);
        }
        return entity;
    }

    private List<CooperationDirectionTagEntity> listActiveTags() {
        return cooperationDirectionTagMapper.selectList(
                new LambdaQueryWrapper<CooperationDirectionTagEntity>()
                        .eq(CooperationDirectionTagEntity::getDeletedMarker, 0L)
                        .orderByAsc(CooperationDirectionTagEntity::getSortOrder)
                        .orderByAsc(CooperationDirectionTagEntity::getId));
    }

    private AdminCooperationDirectionTagVO toAdminVO(CooperationDirectionTagEntity entity) {
        AdminCooperationDirectionTagVO vo = new AdminCooperationDirectionTagVO();
        vo.setId(entity.getId());
        vo.setTagText(StringFieldUtils.defaultString(entity.getTagText()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private PortalCooperationDirectionTagVO toPortalVO(CooperationDirectionTagEntity entity) {
        PortalCooperationDirectionTagVO vo = new PortalCooperationDirectionTagVO();
        vo.setTagText(StringFieldUtils.defaultString(entity.getTagText()));
        return vo;
    }

    private Map<String, Object> toSnapshot(CooperationDirectionTagEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("tagText", entity.getTagText());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private String normalizeTagText(String tagText) {
        String normalized = StringFieldUtils.trimToNull(tagText);
        if (normalized == null) {
            log.warn("cooperation direction tag text blank");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_TAG_TEXT_REQUIRED);
        }
        return normalized;
    }

    private int nextSortOrder() {
        CooperationDirectionTagEntity last = cooperationDirectionTagMapper.selectOne(
                new LambdaQueryWrapper<CooperationDirectionTagEntity>()
                        .eq(CooperationDirectionTagEntity::getDeletedMarker, 0L)
                        .orderByDesc(CooperationDirectionTagEntity::getSortOrder)
                        .orderByDesc(CooperationDirectionTagEntity::getId)
                        .last("limit 1"));
        int current = last == null || last.getSortOrder() == null ? 0 : last.getSortOrder();
        return current + sortGap;
    }

    private int sortOrderForIndex(int index) {
        return (index + 1) * sortGap;
    }

    private List<Long> deduplicateIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        Set<Long> seen = new HashSet<>();
        List<Long> deduplicated = new ArrayList<>();
        for (Long id : ids) {
            if (id == null) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_ORDERED_ID_REQUIRED);
            }
            if (!seen.add(id)) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_DUPLICATE_ORDERED_IDS);
            }
            deduplicated.add(id);
        }
        return deduplicated;
    }

    private Comparator<CooperationDirectionTagEntity> tagComparator() {
        return Comparator
                .comparing(CooperationDirectionTagEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(CooperationDirectionTagEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private void invalidatePortalCache() {
        portalCacheSupport.invalidatePortalKey(CooperationDirectionTagModuleConstants.CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
