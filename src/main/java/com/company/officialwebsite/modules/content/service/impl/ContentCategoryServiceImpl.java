package com.company.officialwebsite.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.content.dto.ContentCategoryBatchSortRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentCategoryCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentCategoryUpdateRequestDTO;
import com.company.officialwebsite.modules.content.entity.ContentCategoryEntity;
import com.company.officialwebsite.modules.content.mapper.ContentCategoryMapper;
import com.company.officialwebsite.modules.content.service.ContentCategoryService;
import com.company.officialwebsite.modules.content.vo.AdminContentCategoryVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
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
public class ContentCategoryServiceImpl implements ContentCategoryService {

    private static final Logger log = LoggerFactory.getLogger(ContentCategoryServiceImpl.class);

    private static final String BIZ_MODULE = "CONTENT";
    private static final String TARGET_TYPE = "CONTENT_CATEGORY";
    private static final String ACTION_CREATE = "CREATE_CONTENT_CATEGORY";
    private static final String ACTION_UPDATE = "UPDATE_CONTENT_CATEGORY";
    private static final String ACTION_DELETE = "DELETE_CONTENT_CATEGORY";
    private static final String ACTION_REORDER = "REORDER_CONTENT_CATEGORY";
    private static final String MSG_NOT_FOUND = "Content category does not exist or has been deleted";
    private static final String MSG_PARENT_NOT_FOUND = "Parent category does not exist or has been deleted";
    private static final String MSG_DUPLICATE = "Category code already exists";
    private static final String MSG_CATEGORY_CODE_REQUIRED = "Category code cannot be empty";
    private static final String MSG_CATEGORY_NAME_REQUIRED = "Category name cannot be empty";
    private static final String MSG_PARENT_SELF = "Category cannot use itself as parent";
    private static final String MSG_PARENT_DESCENDANT = "Category cannot use its descendant as parent";
    private static final String MSG_CHILDREN_EXIST = "Category has child categories and cannot be deleted";
    private static final String MSG_EMPTY_ORDERED_IDS = "Ordered category id list cannot be empty";
    private static final String MSG_INVALID_ORDERED_IDS = "Ordered category id list contains invalid categories";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "Ordered category id list must cover all active categories";
    private static final String MSG_ORDERED_ID_REQUIRED = "Ordered category id cannot be empty";
    private static final String MSG_SORT_ORDER_LIMIT = "Category sort order has reached the limit";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "Category sort order is out of range";

    private final ContentCategoryMapper contentCategoryMapper;
    private final AuditLogService auditLogService;
    private final OfficialProperties officialProperties;

    public ContentCategoryServiceImpl(
            ContentCategoryMapper contentCategoryMapper,
            AuditLogService auditLogService,
            OfficialProperties officialProperties) {
        this.contentCategoryMapper = contentCategoryMapper;
        this.auditLogService = auditLogService;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminContentCategoryVO> getAdminContentCategoryTree() {
        return buildTree(listActiveCategories());
    }

    @Override
    @Transactional
    public void createContentCategory(ContentCategoryCreateRequestDTO requestDTO) {
        Long parentId = normalizeParentId(requestDTO.getParentId());
        if (parentId != null) {
            requireActiveCategory(parentId, MSG_PARENT_NOT_FOUND);
        }

        ContentCategoryEntity entity = new ContentCategoryEntity();
        entity.setParentId(parentId);
        entity.setCategoryCode(normalizeCategoryCode(requestDTO.getCategoryCode()));
        entity.setCategoryName(normalizeCategoryName(requestDTO.getCategoryName()));
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? nextSortOrder(parentId) : requestDTO.getSortOrder());

        try {
            contentCategoryMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create content category duplicate categoryCode={}", entity.getCategoryCode(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        log.info("create content category success id={} categoryCode={}", entity.getId(), entity.getCategoryCode());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void updateContentCategory(Long id, ContentCategoryUpdateRequestDTO requestDTO) {
        ContentCategoryEntity entity = requireActiveCategory(id, MSG_NOT_FOUND);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);

        Long parentId = normalizeParentId(requestDTO.getParentId());
        validateParentChange(entity.getId(), parentId);

        entity.setParentId(parentId);
        entity.setCategoryCode(normalizeCategoryCode(requestDTO.getCategoryCode()));
        entity.setCategoryName(normalizeCategoryName(requestDTO.getCategoryName()));
        entity.setVisible(requestDTO.getVisible() == null || requestDTO.getVisible());
        entity.setSortOrder(requestDTO.getSortOrder() == null ? entity.getSortOrder() : requestDTO.getSortOrder());

        try {
            ConcurrencyHelper.tryUpdate(contentCategoryMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update content category duplicate id={} categoryCode={}", entity.getId(), entity.getCategoryCode(), ex);
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, MSG_DUPLICATE);
        }

        log.info("update content category success id={} categoryCode={}", entity.getId(), entity.getCategoryCode());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
    }

    @Override
    @Transactional
    public void deleteContentCategory(Long id, Integer version) {
        ContentCategoryEntity entity = requireActiveCategory(id, MSG_NOT_FOUND);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Long childCount = contentCategoryMapper.selectCount(
                new LambdaQueryWrapper<ContentCategoryEntity>()
                        .eq(ContentCategoryEntity::getDeletedMarker, 0L)
                        .eq(ContentCategoryEntity::getParentId, entity.getId()));
        if (childCount != null && childCount > 0) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_CHILDREN_EXIST);
        }

        Map<String, Object> before = toSnapshot(entity);
        int deleted = contentCategoryMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        log.info("delete content category success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    @Override
    @Transactional
    public void reorderContentCategories(ContentCategoryBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedCategoryIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_ORDERED_IDS);
        }

        List<ContentCategoryEntity> activeCategories = listActiveCategories();
        if (activeCategories.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }

        Map<Long, ContentCategoryEntity> entityMap = new HashMap<>();
        for (ContentCategoryEntity category : activeCategories) {
            entityMap.put(category.getId(), category);
        }
        if (!entityMap.keySet().equals(new LinkedHashSet<>(requestedOrder))) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }

        List<Map<String, Object>> before = activeCategories.stream()
                .sorted(categoryComparator())
                .map(this::toSnapshot)
                .toList();

        Map<Long, Integer> siblingIndex = new HashMap<>();
        for (Long id : requestedOrder) {
            ContentCategoryEntity entity = entityMap.get(id);
            int index = siblingIndex.merge(parentKey(entity.getParentId()), 1, Integer::sum);
            entity.setSortOrder(sortOrderForIndex(index - 1));
            ConcurrencyHelper.tryUpdate(contentCategoryMapper, entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();

        log.info("reorder content categories success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
    }

    private ContentCategoryEntity requireActiveCategory(Long id, String message) {
        ContentCategoryEntity entity = contentCategoryMapper.selectOne(
                new LambdaQueryWrapper<ContentCategoryEntity>()
                        .eq(ContentCategoryEntity::getId, id)
                        .eq(ContentCategoryEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("content category not found id={}", id);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, message);
        }
        return entity;
    }

    private List<ContentCategoryEntity> listActiveCategories() {
        return contentCategoryMapper.selectList(
                new LambdaQueryWrapper<ContentCategoryEntity>()
                        .eq(ContentCategoryEntity::getDeletedMarker, 0L)
                        .orderByAsc(ContentCategoryEntity::getSortOrder)
                        .orderByAsc(ContentCategoryEntity::getId));
    }

    private List<AdminContentCategoryVO> buildTree(List<ContentCategoryEntity> entities) {
        Map<Long, ContentCategoryEntity> entityMap = new HashMap<>();
        Map<Long, List<ContentCategoryEntity>> childrenMap = new HashMap<>();
        for (ContentCategoryEntity entity : entities) {
            entityMap.put(entity.getId(), entity);
            childrenMap.computeIfAbsent(parentKey(entity.getParentId()), ignored -> new ArrayList<>()).add(entity);
        }
        childrenMap.values().forEach(children -> children.sort(categoryComparator()));

        List<AdminContentCategoryVO> roots = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        List<ContentCategoryEntity> rootEntities = childrenMap.getOrDefault(parentKey(null), List.of());
        for (ContentCategoryEntity root : rootEntities) {
            roots.add(toTreeVO(root, entityMap, childrenMap, visited, 0));
        }

        for (ContentCategoryEntity entity : entities) {
            if (!visited.contains(entity.getId())) {
                roots.add(toTreeVO(entity, entityMap, childrenMap, visited, 0));
            }
        }
        return roots;
    }

    private AdminContentCategoryVO toTreeVO(
            ContentCategoryEntity entity,
            Map<Long, ContentCategoryEntity> entityMap,
            Map<Long, List<ContentCategoryEntity>> childrenMap,
            Set<Long> visited,
            int level) {
        if (!visited.add(entity.getId())) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_PARENT_DESCENDANT);
        }
        AdminContentCategoryVO vo = toAdminVO(entity, entityMap.get(entity.getParentId()), level);
        List<AdminContentCategoryVO> children = childrenMap.getOrDefault(entity.getId(), List.of()).stream()
                .map(child -> toTreeVO(child, entityMap, childrenMap, visited, level + 1))
                .toList();
        vo.setChildren(children);
        return vo;
    }

    private AdminContentCategoryVO toAdminVO(ContentCategoryEntity entity, ContentCategoryEntity parent, int level) {
        AdminContentCategoryVO vo = new AdminContentCategoryVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setParentName(parent == null ? "" : StringFieldUtils.defaultString(parent.getCategoryName()));
        vo.setCategoryCode(StringFieldUtils.defaultString(entity.getCategoryCode()));
        vo.setCategoryName(StringFieldUtils.defaultString(entity.getCategoryName()));
        vo.setTreeName((level <= 0 ? "" : "  ".repeat(level) + "- ") + StringFieldUtils.defaultString(entity.getCategoryName()));
        vo.setVisible(entity.getVisible() == null || entity.getVisible());
        vo.setSortOrder(entity.getSortOrder());
        vo.setLevel(level);
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private void validateParentChange(Long categoryId, Long parentId) {
        if (parentId == null) {
            return;
        }
        if (Objects.equals(categoryId, parentId)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_PARENT_SELF);
        }
        requireActiveCategory(parentId, MSG_PARENT_NOT_FOUND);
        Long cursor = parentId;
        Set<Long> visited = new HashSet<>();
        while (cursor != null) {
            if (!visited.add(cursor)) {
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_PARENT_DESCENDANT);
            }
            if (Objects.equals(cursor, categoryId)) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_PARENT_DESCENDANT);
            }
            ContentCategoryEntity parent = requireActiveCategory(cursor, MSG_PARENT_NOT_FOUND);
            cursor = parent.getParentId();
        }
    }

    private Map<String, Object> toSnapshot(ContentCategoryEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("parentId", entity.getParentId());
        snapshot.put("categoryCode", entity.getCategoryCode());
        snapshot.put("categoryName", entity.getCategoryName());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0 ? null : parentId;
    }

    private String normalizeCategoryCode(String categoryCode) {
        String normalized = StringFieldUtils.trimToNull(categoryCode);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_CATEGORY_CODE_REQUIRED);
        }
        return normalized.toUpperCase(Locale.ROOT);
    }

    private String normalizeCategoryName(String categoryName) {
        String normalized = StringFieldUtils.trimToNull(categoryName);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_CATEGORY_NAME_REQUIRED);
        }
        return normalized;
    }

    private int nextSortOrder(Long parentId) {
        ContentCategoryEntity last = contentCategoryMapper.selectOne(
                new LambdaQueryWrapper<ContentCategoryEntity>()
                        .eq(ContentCategoryEntity::getDeletedMarker, 0L)
                        .eq(parentId != null, ContentCategoryEntity::getParentId, parentId)
                        .isNull(parentId == null, ContentCategoryEntity::getParentId)
                        .orderByDesc(ContentCategoryEntity::getSortOrder)
                        .orderByDesc(ContentCategoryEntity::getId)
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

    private Comparator<ContentCategoryEntity> categoryComparator() {
        return Comparator
                .comparing(ContentCategoryEntity::getParentId, Comparator.nullsFirst(Long::compareTo))
                .thenComparing(ContentCategoryEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ContentCategoryEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private Long parentKey(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    private int sortGap() {
        return Math.max(1, officialProperties.getCache().getSortGap());
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
