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
import com.company.officialwebsite.modules.site.dto.TimelineEventBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.TimelineEventCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.TimelineEventUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.TimelineEventEntity;
import com.company.officialwebsite.modules.site.mapper.TimelineEventMapper;
import com.company.officialwebsite.modules.site.service.TimelineEventService;
import com.company.officialwebsite.modules.site.vo.AdminTimelineEventVO;
import com.company.officialwebsite.modules.site.vo.PortalTimelineEventVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
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
 * TimelineEventServiceImpl：实现时间轴节点的后台维护、审计和前台缓存逻辑。
 */
@Service
public class TimelineEventServiceImpl implements TimelineEventService {

    private static final Logger log = LoggerFactory.getLogger(TimelineEventServiceImpl.class);

    private static final String CACHE_SEGMENT = "timeline_events";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "TIMELINE";
    private static final String ACTION_CREATE = "CREATE_TIMELINE";
    private static final String ACTION_UPDATE = "UPDATE_TIMELINE";
    private static final String ACTION_DELETE = "DELETE_TIMELINE";
    private static final String ACTION_REORDER = "REORDER_TIMELINE";
    private static final String MSG_EMPTY_REORDER_IDS = "排序节点列表不能为空";
    private static final String MSG_INVALID_REORDER_IDS = "排序列表包含不存在或已删除的节点";
    private static final String MSG_CROSS_YEAR_REORDER = "批量重排仅允许同一年份内的节点";
    private static final String MSG_INCOMPLETE_REORDER_IDS = "排序列表必须完整覆盖该年份下全部节点";
    private static final String MSG_TITLE_REQUIRED = "标题不能为空";
    private static final String MSG_TITLE_TOO_LONG = "标题长度不能超过128个字符";
    private static final String MSG_DESCRIPTION_REQUIRED = "描述不能为空";
    private static final String MSG_DESCRIPTION_TOO_LONG = "描述长度不能超过512个字符";
    private static final String MSG_SORT_ORDER_OVERFLOW = "时间轴节点排序值已达到上限";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "排序值超出允许范围";
    private static final String MSG_REORDER_ID_REQUIRED = "排序节点 ID 不能为空";

    private static final int MIN_YEAR = 1900;
    private static final int MAX_YEAR = 2100;
    private static final int MAX_TITLE_LENGTH = 128;
    private static final int MAX_DESCRIPTION_LENGTH = 512;

    private final TimelineEventMapper timelineEventMapper;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final int sortGap;

    public TimelineEventServiceImpl(
            TimelineEventMapper timelineEventMapper,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport) {
        this.timelineEventMapper = timelineEventMapper;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminTimelineEventVO> getAdminTimelineEventList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);
        Page<TimelineEventEntity> page = timelineEventMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<TimelineEventEntity>()
                        .eq(TimelineEventEntity::getDeletedMarker, 0L)
                        .orderByAsc(TimelineEventEntity::getEventYear)
                        .orderByAsc(TimelineEventEntity::getSortOrder)
                        .orderByAsc(TimelineEventEntity::getId));
        List<AdminTimelineEventVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createTimelineEvent(TimelineEventCreateRequestDTO requestDTO) {
        TimelineEventEntity entity = new TimelineEventEntity();
        applyForCreate(entity, requestDTO);
        try {
            timelineEventMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create timeline event duplicate year={} title={}", entity.getEventYear(), entity.getTitle(), ex);
            throw new BusinessException(ErrorCode.SITE_TIMELINE_TITLE_DUPLICATE);
        }
        log.info("create timeline event success id={} year={} title={}", entity.getId(), entity.getEventYear(), entity.getTitle());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void updateTimelineEvent(Long id, TimelineEventUpdateRequestDTO requestDTO) {
        TimelineEventEntity entity = requireActiveTimelineEvent(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        applyForUpdate(entity, requestDTO);
        try {
            ConcurrencyHelper.tryUpdate(timelineEventMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update timeline event duplicate id={} year={} title={}",
                    entity.getId(), entity.getEventYear(), entity.getTitle(), ex);
            throw new BusinessException(ErrorCode.SITE_TIMELINE_TITLE_DUPLICATE);
        }
        log.info("update timeline event success id={} year={} title={}", entity.getId(), entity.getEventYear(), entity.getTitle());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void deleteTimelineEvent(Long id, Integer version) {
        TimelineEventEntity entity = requireActiveTimelineEvent(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = timelineEventMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete timeline event success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void reorderTimelineEvents(TimelineEventBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedTimelineEventIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_REORDER_IDS);
        }

        List<TimelineEventEntity> targetEntities = timelineEventMapper.selectList(
                new LambdaQueryWrapper<TimelineEventEntity>()
                        .eq(TimelineEventEntity::getDeletedMarker, 0L)
                        .in(TimelineEventEntity::getId, requestedOrder));
        if (targetEntities.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_REORDER_IDS);
        }

        Integer year = targetEntities.get(0).getEventYear();
        if (!targetEntities.stream().map(TimelineEventEntity::getEventYear).allMatch(y -> Objects.equals(y, year))) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_CROSS_YEAR_REORDER);
        }

        List<TimelineEventEntity> activeInYear = listActiveByYear(year);
        Set<Long> currentIds = new LinkedHashSet<>(activeInYear.stream()
                .map(TimelineEventEntity::getId)
                .toList());
        if (!new LinkedHashSet<>(requestedOrder).equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_REORDER_IDS);
        }

        Map<Long, TimelineEventEntity> entityMap = new HashMap<>();
        for (TimelineEventEntity event : activeInYear) {
            entityMap.put(event.getId(), event);
        }

        List<Map<String, Object>> before = activeInYear.stream()
                .sorted(timelineComparator())
                .map(this::toSnapshot)
                .toList();

        for (int index = 0; index < requestedOrder.size(); index++) {
            TimelineEventEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            ConcurrencyHelper.tryUpdate(timelineEventMapper, entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();

        log.info("reorder timeline events success year={} count={} order={}", year, requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalTimelineEventVO> getPortalTimelineEvents() {
        String cacheKey = portalCacheSupport.buildKey(CACHE_SEGMENT);
        List<PortalTimelineEventVO> cached = portalCacheSupport.readListCache(cacheKey, PortalTimelineEventVO.class, CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        List<PortalTimelineEventVO> events = listVisibleEvents().stream()
                .map(this::toPortalVO)
                .toList();
        portalCacheSupport.writeCache(cacheKey, events, portalCacheSupport.isEmptyResult(events), CACHE_SEGMENT);
        return events;
    }

    private void applyForCreate(TimelineEventEntity entity, TimelineEventCreateRequestDTO requestDTO) {
        entity.setEventYear(assertValidYear(requestDTO.getYear()));
        entity.setTitle(normalizeTitle(requestDTO.getTitle()));
        entity.setDescription(normalizeDescription(requestDTO.getDescription()));
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(nextSortOrder(entity.getEventYear()));
    }

    private void applyForUpdate(TimelineEventEntity entity, TimelineEventUpdateRequestDTO requestDTO) {
        Integer newYear = assertValidYear(requestDTO.getYear());
        if (!Objects.equals(entity.getEventYear(), newYear)) {
            entity.setEventYear(newYear);
            entity.setSortOrder(nextSortOrder(newYear));
        }
        entity.setTitle(normalizeTitle(requestDTO.getTitle()));
        entity.setDescription(normalizeDescription(requestDTO.getDescription()));
        entity.setVisible(requestDTO.getVisible());
    }

    private TimelineEventEntity requireActiveTimelineEvent(Long id) {
        TimelineEventEntity entity = timelineEventMapper.selectOne(
                new LambdaQueryWrapper<TimelineEventEntity>()
                        .eq(TimelineEventEntity::getId, id)
                        .eq(TimelineEventEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("timeline event not found id={}", id);
            throw new BusinessException(ErrorCode.SITE_TIMELINE_NOT_FOUND);
        }
        return entity;
    }

    private List<TimelineEventEntity> listActiveByYear(Integer year) {
        return timelineEventMapper.selectList(
                new LambdaQueryWrapper<TimelineEventEntity>()
                        .eq(TimelineEventEntity::getDeletedMarker, 0L)
                        .eq(TimelineEventEntity::getEventYear, year)
                        .orderByAsc(TimelineEventEntity::getSortOrder)
                        .orderByAsc(TimelineEventEntity::getId));
    }

    private List<TimelineEventEntity> listVisibleEvents() {
        return timelineEventMapper.selectList(
                new LambdaQueryWrapper<TimelineEventEntity>()
                        .eq(TimelineEventEntity::getDeletedMarker, 0L)
                        .eq(TimelineEventEntity::getVisible, true)
                        .orderByAsc(TimelineEventEntity::getEventYear)
                        .orderByAsc(TimelineEventEntity::getSortOrder)
                        .orderByAsc(TimelineEventEntity::getId));
    }

    private AdminTimelineEventVO toAdminVO(TimelineEventEntity entity) {
        AdminTimelineEventVO vo = new AdminTimelineEventVO();
        vo.setId(entity.getId());
        vo.setYear(entity.getEventYear());
        vo.setTitle(StringFieldUtils.defaultString(entity.getTitle()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private PortalTimelineEventVO toPortalVO(TimelineEventEntity entity) {
        PortalTimelineEventVO vo = new PortalTimelineEventVO();
        vo.setYear(entity.getEventYear());
        vo.setTitle(StringFieldUtils.defaultString(entity.getTitle()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        return vo;
    }

    private Map<String, Object> toSnapshot(TimelineEventEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("year", entity.getEventYear());
        snapshot.put("title", entity.getTitle());
        snapshot.put("description", entity.getDescription());
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private Integer assertValidYear(Integer year) {
        if (year == null || year < MIN_YEAR || year > MAX_YEAR) {
            log.warn("timeline event year invalid year={} allowedRange=[{}, {}]", year, MIN_YEAR, MAX_YEAR);
            throw new BusinessException(ErrorCode.SITE_TIMELINE_YEAR_INVALID);
        }
        return year;
    }

    private String normalizeTitle(String title) {
        String normalized = StringFieldUtils.trimToNull(title);
        if (normalized == null) {
            log.warn("timeline event title blank after trim");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_TITLE_REQUIRED);
        }
        if (normalized.length() > MAX_TITLE_LENGTH) {
            log.warn("timeline event title too long length={} max={}", normalized.length(), MAX_TITLE_LENGTH);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_TITLE_TOO_LONG);
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        String normalized = StringFieldUtils.trimToNull(description);
        if (normalized == null) {
            log.warn("timeline event description blank after trim");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_DESCRIPTION_REQUIRED);
        }
        if (normalized.length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("timeline event description too long length={} max={}",
                    normalized.length(), MAX_DESCRIPTION_LENGTH);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_DESCRIPTION_TOO_LONG);
        }
        return normalized;
    }

    private int nextSortOrder(Integer year) {
        TimelineEventEntity last = timelineEventMapper.selectOne(
                new LambdaQueryWrapper<TimelineEventEntity>()
                        .eq(TimelineEventEntity::getDeletedMarker, 0L)
                        .eq(TimelineEventEntity::getEventYear, year)
                        .orderByDesc(TimelineEventEntity::getSortOrder)
                        .orderByDesc(TimelineEventEntity::getId)
                        .last("limit 1"));
        int current = last == null || last.getSortOrder() == null ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_SORT_ORDER_OVERFLOW);
        }
        return current + sortGap;
    }

    private int sortOrderForIndex(int index) {
        try {
            return Math.multiplyExact(Math.addExact(index, 1), sortGap);
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
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_REORDER_ID_REQUIRED);
            }
            deduplicated.add(id);
        }
        return List.copyOf(deduplicated);
    }

    private Comparator<TimelineEventEntity> timelineComparator() {
        return Comparator
                .comparing(TimelineEventEntity::getEventYear, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TimelineEventEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(TimelineEventEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private void invalidatePortalCache() {
        portalCacheSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
