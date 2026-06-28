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
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.dto.ValueCardBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.ValueCardCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.ValueCardUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.ValueCardEntity;
import com.company.officialwebsite.modules.site.mapper.ValueCardMapper;
import com.company.officialwebsite.modules.site.service.ValueCardService;
import com.company.officialwebsite.modules.site.vo.AdminValueCardVO;
import com.company.officialwebsite.modules.site.vo.PortalValueCardVO;
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
 * ValueCardServiceImpl：实现核心价值观卡片的后台维护、审计和前台缓存逻辑。
 */
@Service
public class ValueCardServiceImpl implements ValueCardService {

    private static final Logger log = LoggerFactory.getLogger(ValueCardServiceImpl.class);

    private static final String CACHE_SEGMENT = "value_cards";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "VALUE_CARD";
    private static final String ACTION_CREATE = "CREATE_VALUE_CARD";
    private static final String ACTION_UPDATE = "UPDATE_VALUE_CARD";
    private static final String ACTION_DELETE = "DELETE_VALUE_CARD";
    private static final String ACTION_REORDER = "REORDER_VALUE_CARD";
    private static final String MEDIA_BIZ_FIELD = "icon";
    private static final String MSG_EMPTY_ORDERED_IDS = "排序卡片列表不能为空";
    private static final String MSG_INVALID_ORDERED_IDS = "排序列表包含不存在或已删除的卡片";
    private static final String MSG_INCOMPLETE_ORDERED_IDS = "排序列表必须完整覆盖全部活跃卡片";
    private static final String MSG_TITLE_REQUIRED = "标题不能为空";
    private static final String MSG_SUBTITLE_REQUIRED = "副标语不能为空";
    private static final String MSG_DESCRIPTION_REQUIRED = "描述不能为空";
    private static final String MSG_SORT_ORDER_LIMIT = "核心价值观卡片排序值已达到上限";
    private static final String MSG_SORT_ORDER_OUT_OF_RANGE = "排序值超出允许范围";
    private static final String MSG_ORDERED_ID_REQUIRED = "排序卡片 ID 不能为空";

    private final ValueCardMapper valueCardMapper;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final int sortGap;

    public ValueCardServiceImpl(
            ValueCardMapper valueCardMapper,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport) {
        this.valueCardMapper = valueCardMapper;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminValueCardVO> getAdminValueCardList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);
        Page<ValueCardEntity> page = valueCardMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<ValueCardEntity>()
                        .eq(ValueCardEntity::getDeletedMarker, 0L)
                        .orderByAsc(ValueCardEntity::getSortOrder)
                        .orderByAsc(ValueCardEntity::getId));
        List<AdminValueCardVO> list = page.getRecords().stream()
                .map(this::toAdminVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public void createValueCard(ValueCardCreateRequestDTO requestDTO) {
        ValueCardEntity entity = new ValueCardEntity();
        applyForCreate(entity, requestDTO);
        try {
            valueCardMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create value card duplicate title={}", entity.getTitle(), ex);
            throw new BusinessException(ErrorCode.SITE_VALUE_CARD_TITLE_DUPLICATE);
        }
        mediaAssetService.bindMedia(entity.getIconMediaId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        log.info("create value card success id={} title={}", entity.getId(), entity.getTitle());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void updateValueCard(Long id, ValueCardUpdateRequestDTO requestDTO) {
        ValueCardEntity entity = requireActiveValueCard(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        Long oldIconMediaId = entity.getIconMediaId();
        applyForUpdate(entity, requestDTO);
        try {
            ConcurrencyHelper.tryUpdate(valueCardMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update value card duplicate id={} title={}", entity.getId(), entity.getTitle(), ex);
            throw new BusinessException(ErrorCode.SITE_VALUE_CARD_TITLE_DUPLICATE);
        }
        if (!Objects.equals(oldIconMediaId, entity.getIconMediaId())) {
            handleIconBinding(oldIconMediaId, entity.getIconMediaId(), entity.getId());
        }
        log.info("update value card success id={} title={}", entity.getId(), entity.getTitle());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void deleteValueCard(Long id, Integer version) {
        ValueCardEntity entity = requireActiveValueCard(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = valueCardMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        handleIconBinding(entity.getIconMediaId(), null, entity.getId());
        log.info("delete value card success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void reorderValueCards(ValueCardBatchSortRequestDTO requestDTO) {
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedValueCardIds());
        if (requestedOrder.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMPTY_ORDERED_IDS);
        }

        List<ValueCardEntity> targetEntities = valueCardMapper.selectList(
                new LambdaQueryWrapper<ValueCardEntity>()
                        .eq(ValueCardEntity::getDeletedMarker, 0L)
                        .in(ValueCardEntity::getId, requestedOrder));
        if (targetEntities.size() != requestedOrder.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_INVALID_ORDERED_IDS);
        }

        List<ValueCardEntity> activeCards = listActiveCards();
        Set<Long> currentIds = new LinkedHashSet<>(activeCards.stream()
                .map(ValueCardEntity::getId)
                .toList());
        if (!new LinkedHashSet<>(requestedOrder).equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_INCOMPLETE_ORDERED_IDS);
        }

        Map<Long, ValueCardEntity> entityMap = new HashMap<>();
        for (ValueCardEntity card : activeCards) {
            entityMap.put(card.getId(), card);
        }

        List<Map<String, Object>> before = activeCards.stream()
                .sorted(cardComparator())
                .map(this::toSnapshot)
                .toList();

        for (int index = 0; index < requestedOrder.size(); index++) {
            ValueCardEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            ConcurrencyHelper.tryUpdate(valueCardMapper, entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();

        log.info("reorder value cards success count={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalValueCardVO> getPortalValueCards() {
        String cacheKey = portalCacheSupport.buildKey(CACHE_SEGMENT);
        List<PortalValueCardVO> cached = portalCacheSupport.readListCache(cacheKey, PortalValueCardVO.class, CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        List<PortalValueCardVO> cards = listVisibleCards().stream()
                .map(this::toPortalVO)
                .toList();
        portalCacheSupport.writeCache(cacheKey, cards, portalCacheSupport.isEmptyResult(cards), CACHE_SEGMENT);
        return cards;
    }

    private void applyForCreate(ValueCardEntity entity, ValueCardCreateRequestDTO requestDTO) {
        try {
            mediaAssetService.requireUsableImage(requestDTO.getIconMediaId());
        } catch (BusinessException ex) {
            log.warn("create value card icon invalid iconMediaId={}", requestDTO.getIconMediaId(), ex);
            throw new BusinessException(ErrorCode.SITE_VALUE_CARD_ICON_INVALID);
        }
        entity.setIconMediaId(requestDTO.getIconMediaId());
        entity.setTitle(normalizeTitle(requestDTO.getTitle()));
        entity.setSubtitle(normalizeSubtitle(requestDTO.getSubtitle()));
        entity.setDescription(normalizeDescription(requestDTO.getDescription()));
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(nextSortOrder());
        assertTitleUnique(entity.getTitle(), null);
    }

    private void applyForUpdate(ValueCardEntity entity, ValueCardUpdateRequestDTO requestDTO) {
        try {
            mediaAssetService.requireUsableImage(requestDTO.getIconMediaId());
        } catch (BusinessException ex) {
            log.warn("update value card icon invalid id={} iconMediaId={}",
                    entity.getId(), requestDTO.getIconMediaId(), ex);
            throw new BusinessException(ErrorCode.SITE_VALUE_CARD_ICON_INVALID);
        }
        entity.setIconMediaId(requestDTO.getIconMediaId());
        entity.setTitle(normalizeTitle(requestDTO.getTitle()));
        entity.setSubtitle(normalizeSubtitle(requestDTO.getSubtitle()));
        entity.setDescription(normalizeDescription(requestDTO.getDescription()));
        entity.setVisible(requestDTO.getVisible());
        assertTitleUnique(entity.getTitle(), entity.getId());
    }

    private ValueCardEntity requireActiveValueCard(Long id) {
        ValueCardEntity entity = valueCardMapper.selectOne(
                new LambdaQueryWrapper<ValueCardEntity>()
                        .eq(ValueCardEntity::getId, id)
                        .eq(ValueCardEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("value card not found id={}", id);
            throw new BusinessException(ErrorCode.SITE_VALUE_CARD_NOT_FOUND);
        }
        return entity;
    }

    private List<ValueCardEntity> listActiveCards() {
        return valueCardMapper.selectList(
                new LambdaQueryWrapper<ValueCardEntity>()
                        .eq(ValueCardEntity::getDeletedMarker, 0L)
                        .orderByAsc(ValueCardEntity::getSortOrder)
                        .orderByAsc(ValueCardEntity::getId));
    }

    private List<ValueCardEntity> listVisibleCards() {
        return valueCardMapper.selectList(
                new LambdaQueryWrapper<ValueCardEntity>()
                        .eq(ValueCardEntity::getDeletedMarker, 0L)
                        .eq(ValueCardEntity::getVisible, true)
                        .orderByAsc(ValueCardEntity::getSortOrder)
                        .orderByAsc(ValueCardEntity::getId));
    }

    private AdminValueCardVO toAdminVO(ValueCardEntity entity) {
        AdminValueCardVO vo = new AdminValueCardVO();
        vo.setId(entity.getId());
        vo.setIconMediaId(entity.getIconMediaId());
        vo.setIconUrl(resolveIconUrl(entity.getIconMediaId(), entity.getId()));
        vo.setTitle(StringFieldUtils.defaultString(entity.getTitle()));
        vo.setSubtitle(StringFieldUtils.defaultString(entity.getSubtitle()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private PortalValueCardVO toPortalVO(ValueCardEntity entity) {
        PortalValueCardVO vo = new PortalValueCardVO();
        vo.setIconUrl(resolveIconUrl(entity.getIconMediaId(), entity.getId()));
        vo.setTitle(StringFieldUtils.defaultString(entity.getTitle()));
        vo.setSubtitle(StringFieldUtils.defaultString(entity.getSubtitle()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        return vo;
    }

    private String resolveIconUrl(Long iconMediaId, Long cardId) {
        if (iconMediaId == null) {
            return "";
        }
        try {
            MediaAssetEntity asset = mediaAssetService.requireUsableImage(iconMediaId);
            return StringFieldUtils.defaultString(asset.getPublicUrl());
        } catch (BusinessException ex) {
            log.warn("value card icon unavailable cardId={} iconMediaId={}", cardId, iconMediaId);
            return "";
        }
    }

    private Map<String, Object> toSnapshot(ValueCardEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("iconMediaId", entity.getIconMediaId());
        snapshot.put("title", entity.getTitle());
        snapshot.put("subtitle", entity.getSubtitle());
        snapshot.put("description", entity.getDescription());
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void assertTitleUnique(String title, Long excludeId) {
        Long count = valueCardMapper.selectCount(
                new LambdaQueryWrapper<ValueCardEntity>()
                        .eq(ValueCardEntity::getTitle, title)
                        .eq(ValueCardEntity::getDeletedMarker, 0L)
                        .ne(excludeId != null, ValueCardEntity::getId, excludeId));
        if (count != null && count > 0) {
            log.warn("value card title duplicate title={} excludeId={}", title, excludeId);
            throw new BusinessException(ErrorCode.SITE_VALUE_CARD_TITLE_DUPLICATE);
        }
    }

    private String normalizeTitle(String title) {
        String normalized = StringFieldUtils.trimToNull(title);
        if (normalized == null) {
            log.warn("value card title blank");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_TITLE_REQUIRED);
        }
        return normalized;
    }

    private String normalizeSubtitle(String subtitle) {
        String normalized = StringFieldUtils.trimToNull(subtitle);
        if (normalized == null) {
            log.warn("value card subtitle blank");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_SUBTITLE_REQUIRED);
        }
        return normalized;
    }

    private String normalizeDescription(String description) {
        String normalized = StringFieldUtils.trimToNull(description);
        if (normalized == null) {
            log.warn("value card description blank");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_DESCRIPTION_REQUIRED);
        }
        return normalized;
    }

    private int nextSortOrder() {
        ValueCardEntity last = valueCardMapper.selectOne(
                new LambdaQueryWrapper<ValueCardEntity>()
                        .eq(ValueCardEntity::getDeletedMarker, 0L)
                        .orderByDesc(ValueCardEntity::getSortOrder)
                        .orderByDesc(ValueCardEntity::getId)
                        .last("limit 1"));
        int current = last == null || last.getSortOrder() == null ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_SORT_ORDER_LIMIT);
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
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_ORDERED_ID_REQUIRED);
            }
            deduplicated.add(id);
        }
        return List.copyOf(deduplicated);
    }

    private Comparator<ValueCardEntity> cardComparator() {
        return Comparator
                .comparing(ValueCardEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(ValueCardEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private void handleIconBinding(Long oldIconMediaId, Long newIconMediaId, Long bizObjectId) {
        if (!Objects.equals(oldIconMediaId, newIconMediaId)) {
            mediaAssetService.bindMedia(newIconMediaId, BIZ_MODULE, bizObjectId, MEDIA_BIZ_FIELD);
        }
    }

    private void invalidatePortalCache() {
        portalCacheSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
