package com.company.officialwebsite.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.content.dto.ContentTagBatchSortRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentTagCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentTagUpdateRequestDTO;
import com.company.officialwebsite.modules.content.entity.ContentTagEntity;
import com.company.officialwebsite.modules.content.mapper.ContentTagMapper;
import com.company.officialwebsite.modules.content.service.ContentTagService;
import com.company.officialwebsite.modules.content.vo.AdminContentTagVO;
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
public class ContentTagServiceImpl implements ContentTagService {

    private static final Logger log = LoggerFactory.getLogger(ContentTagServiceImpl.class);

    private static final String BIZ_MODULE = "CONTENT";
    private static final String TARGET_TYPE = "CONTENT_TAG";
    private static final String ACTION_CREATE = "CREATE_CONTENT_TAG";
    private static final String ACTION_UPDATE = "UPDATE_CONTENT_TAG";
    private static final String ACTION_DELETE = "DELETE_CONTENT_TAG";
    private static final String ACTION_REORDER = "REORDER_CONTENT_TAG";
    private static final String MSG_NOT_FOUND = "Content tag does not exist or has been deleted";
    private static final String MSG_DUPLICATE = "Tag code or tag name already exists";
    private static final String MSG_TAG_CODE_REQUIRED = "Tag code cannot be empty";
    private static final String MSG_TAG_NAME_REQUIRED = "Tag name cannot be empty";
    private static final String MSG_EMPTY_ORDERED_IDS = "Ordered tag id list cannot be empty";
    private static final String MSG_INVALID_ORDERED_IDS = "Ordered tag id list contains invalid tags";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "Ordered tag id list must cover all active tags";
    private static final String MSG_ORDERED_ID_REQUIRED = "Ordered tag id cannot be empty";
    private static final String MSG_SORT_ORDER_LIMIT = "Tag sort order has reached the limit";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "Tag sort order is out of range";

    private final ContentTagMapper contentTagMapper;
    private final AuditLogService auditLogService;
    private final OfficialProperties officialProperties;

    public ContentTagServiceImpl(
            ContentTagMapper contentTagMapper,
            AuditLogService auditLogService,
            OfficialProperties officialProperties) {
        this.contentTagMapper = contentTagMapper;
        this.auditLogService = auditLogService;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminContentTagVO> getAdminContentTagList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 200);
        Page<ContentTagEntity> page = contentTagMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<ContentTagEntity>()
                        .eq(ContentTagEntity::getDeletedMarker, 0L)
                        .orderByAsc(ContentTagEntity::getSortOrder)
                        .orderByAsc(ContentTagEntity::getId));
        List<AdminContentTagVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createContentTag(ContentTagCreateRequestDTO requestDTO) {
        ContentTagEntity entity = new ContentTagEntity();
        entity.setTagCode(normalizeTagCode(requestDTO.getTagCode()));
        entity.setTagName(normalizeTagName(requestDTO.getTagName()));
        entity.setDescription(StringFieldUtils.trimToNull(requestDTO.getDescription()));
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? nextSortOrder() : requestDTO.getSortOrder());

        try {
            contentTagMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create content tag duplicate tagCode={} tagName={}", entity.getTagCode(), entity.getTagName(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        log.info("create content tag success id={} tagCode={}", entity.getId(), entity.getTagCode());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void updateContentTag(Long id, ContentTagUpdateRequestDTO requestDTO) {
        ContentTagEntity entity = requireActiveTag(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);

        entity.setTagCode(normalizeTagCode(requestDTO.getTagCode()));
        entity.setTagName(normalizeTagName(requestDTO.getTagName()));
        entity.setDescription(StringFieldUtils.trimToNull(requestDTO.getDescription()));
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? entity.getSortOrder() : requestDTO.getSortOrder());

        try {
            ConcurrencyHelper.tryUpdate(contentTagMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update content tag duplicate id={} tagCode={} tagName={}",
                    entity.getId(), entity.getTagCode(), entity.getTagName(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        log.info("update content tag success id={} tagCode={}", entity.getId(), entity.getTagCode());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void deleteContentTag(Long id, Integer version) {
        ContentTagEntity entity = requireActiveTag(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = contentTagMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete content tag success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    @Override
    @Transactional
    public void reorderContentTags(ContentTagBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedTagIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_ORDERED_IDS);
        }

        List<ContentTagEntity> targetEntities = contentTagMapper.selectList(
                new LambdaQueryWrapper<ContentTagEntity>()
                        .eq(ContentTagEntity::getDeletedMarker, 0L)
                        .in(ContentTagEntity::getId, requestedOrder));
        if (targetEntities.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }

        List<ContentTagEntity> activeTags = listActiveTags();
        Set<Long> currentIds = new LinkedHashSet<>(activeTags.stream()
                .map(ContentTagEntity::getId)
                .toList());
        if (!new LinkedHashSet<>(requestedOrder).equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }

        Map<Long, ContentTagEntity> entityMap = new HashMap<>();
        for (ContentTagEntity tag : activeTags) {
            entityMap.put(tag.getId(), tag);
        }

        List<Map<String, Object>> before = activeTags.stream()
                .sorted(tagComparator())
                .map(this::toSnapshot)
                .toList();

        for (int index = 0; index < requestedOrder.size(); index++) {
            ContentTagEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            ConcurrencyHelper.tryUpdate(contentTagMapper, entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();

        log.info("reorder content tags success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
    }

    private ContentTagEntity requireActiveTag(Long id) {
        ContentTagEntity entity = contentTagMapper.selectOne(
                new LambdaQueryWrapper<ContentTagEntity>()
                        .eq(ContentTagEntity::getId, id)
                        .eq(ContentTagEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("content tag not found id={}", id);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, MSG_NOT_FOUND);
        }
        return entity;
    }

    private List<ContentTagEntity> listActiveTags() {
        return contentTagMapper.selectList(
                new LambdaQueryWrapper<ContentTagEntity>()
                        .eq(ContentTagEntity::getDeletedMarker, 0L)
                        .orderByAsc(ContentTagEntity::getSortOrder)
                        .orderByAsc(ContentTagEntity::getId));
    }

    private AdminContentTagVO toAdminVO(ContentTagEntity entity) {
        AdminContentTagVO vo = new AdminContentTagVO();
        vo.setId(entity.getId());
        vo.setTagCode(StringFieldUtils.defaultString(entity.getTagCode()));
        vo.setTagName(StringFieldUtils.defaultString(entity.getTagName()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setVisible(entity.getVisible() == null || entity.getVisible());
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Map<String, Object> toSnapshot(ContentTagEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("tagCode", entity.getTagCode());
        snapshot.put("tagName", entity.getTagName());
        snapshot.put("description", entity.getDescription());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private String normalizeTagCode(String tagCode) {
        String normalized = StringFieldUtils.trimToNull(tagCode);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_TAG_CODE_REQUIRED);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeTagName(String tagName) {
        String normalized = StringFieldUtils.trimToNull(tagName);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_TAG_NAME_REQUIRED);
        }
        return normalized;
    }

    private int nextSortOrder() {
        ContentTagEntity last = contentTagMapper.selectOne(
                new LambdaQueryWrapper<ContentTagEntity>()
                        .eq(ContentTagEntity::getDeletedMarker, 0L)
                        .orderByDesc(ContentTagEntity::getSortOrder)
                        .orderByDesc(ContentTagEntity::getId)
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

    private Comparator<ContentTagEntity> tagComparator() {
        return Comparator
                .comparing(ContentTagEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ContentTagEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private int sortGap() {
        return Math.max(1, officialProperties.getCache().getSortGap());
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
