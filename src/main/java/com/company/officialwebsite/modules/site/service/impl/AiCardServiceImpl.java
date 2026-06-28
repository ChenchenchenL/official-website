package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.converter.AiCardConverter;
import com.company.officialwebsite.modules.site.dto.AiCardCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.AiCardSortItemDTO;
import com.company.officialwebsite.modules.site.dto.AiCardUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.AiCardEntity;
import com.company.officialwebsite.modules.site.mapper.AiCardMapper;
import com.company.officialwebsite.modules.site.service.AiCardService;
import com.company.officialwebsite.modules.site.vo.AdminAiCardVO;
import com.company.officialwebsite.modules.site.vo.PortalAiCardVO;
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
 * AiCardServiceImpl：实现 AI 战略卡片的 CRUD 维护、排序、前台缓存与后台操作审计逻辑。
 */
@Service
public class AiCardServiceImpl implements AiCardService {

    private static final Logger log = LoggerFactory.getLogger(AiCardServiceImpl.class);

    private static final String CACHE_SEGMENT = "ai_cards";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "AI_CARD";
    private static final String ACTION_CREATE = "CREATE_AI_CARD";
    private static final String ACTION_UPDATE = "UPDATE_AI_CARD";
    private static final String ACTION_DELETE = "DELETE_AI_CARD";
    private static final String ACTION_REORDER = "REORDER_AI_CARD";
    private static final String MEDIA_BIZ_FIELD = "icon";

    private final AiCardMapper aiCardMapper;
    private final AiCardConverter aiCardConverter;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final int sortGap;

    public AiCardServiceImpl(
            AiCardMapper aiCardMapper,
            AiCardConverter aiCardConverter,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport) {
        this.aiCardMapper = aiCardMapper;
        this.aiCardConverter = aiCardConverter;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminAiCardVO> getAdminCards(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);
        Page<AiCardEntity> page = aiCardMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<AiCardEntity>()
                        .eq(AiCardEntity::getDeletedMarker, 0L)
                        .orderByAsc(AiCardEntity::getSortOrder)
                        .orderByAsc(AiCardEntity::getId));
        List<AdminAiCardVO> list = page.getRecords().stream().map(aiCardConverter::toAdminVO).toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public Long createCard(AiCardCreateRequestDTO requestDTO) {
        AiCardEntity entity = new AiCardEntity();
        entity.setName(requestDTO.getName().trim());
        entity.setEnglishName(requestDTO.getEnglishName() != null ? requestDTO.getEnglishName().trim() : null);
        entity.setDescription(requestDTO.getDescription().trim());
        entity.setJumpLink(requestDTO.getJumpLink() != null ? requestDTO.getJumpLink().trim() : null);
        entity.setVisible(requestDTO.getVisible());

        if (requestDTO.getIconId() != null) {
            try {
                mediaAssetService.requireUsableImage(requestDTO.getIconId());
            } catch (BusinessException ex) {
                log.warn("ai card icon validation failed iconId={}", requestDTO.getIconId());
                throw new BusinessException(ErrorCode.SITE_AI_CARD_ICON_INVALID);
            }
            entity.setIconId(requestDTO.getIconId());
        }

        if (requestDTO.getSortOrder() != null) {
            entity.setSortOrder(requestDTO.getSortOrder());
        } else {
            entity.setSortOrder(nextSortOrder());
        }

        try {
            aiCardMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_AI_CARD_NAME_DUPLICATE);
        }

        if (entity.getIconId() != null) {
            try {
                mediaAssetService.requireUsableImage(entity.getIconId());
            } catch (BusinessException ex) {
                log.warn("ai card icon validation failed before binding iconId={}", entity.getIconId());
                throw new BusinessException(ErrorCode.SITE_AI_CARD_ICON_INVALID);
            }
            mediaAssetService.bindMedia(entity.getIconId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        }

        log.info("create ai card success cardId={} name={} sortOrder={}",
                entity.getId(), entity.getName(), entity.getSortOrder());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateCard(Long id, AiCardUpdateRequestDTO requestDTO) {
        AiCardEntity entity = requireActiveCard(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        Long oldIconId = entity.getIconId();

        entity.setName(requestDTO.getName().trim());
        entity.setEnglishName(requestDTO.getEnglishName() != null ? requestDTO.getEnglishName().trim() : null);
        entity.setDescription(requestDTO.getDescription().trim());
        entity.setJumpLink(requestDTO.getJumpLink() != null ? requestDTO.getJumpLink().trim() : null);
        entity.setVisible(requestDTO.getVisible());
        if (requestDTO.getSortOrder() != null) {
            entity.setSortOrder(requestDTO.getSortOrder());
        }

        // iconId 必传，校验并设置
        try {
            mediaAssetService.requireUsableImage(requestDTO.getIconId());
        } catch (BusinessException ex) {
            log.warn("ai card icon validation failed iconId={}", requestDTO.getIconId());
            throw new BusinessException(ErrorCode.SITE_AI_CARD_ICON_INVALID);
        }
        entity.setIconId(requestDTO.getIconId());

        try {
            ConcurrencyHelper.tryUpdate(aiCardMapper, entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_AI_CARD_NAME_DUPLICATE);
        }

        if (!Objects.equals(oldIconId, entity.getIconId())) {
            handleIconBinding(oldIconId, entity.getIconId(), entity.getId());
        }

        log.info("update ai card success cardId={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void deleteCard(Long id, Integer version) {
        AiCardEntity entity = requireActiveCard(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);

        int deleted = aiCardMapper.delete(
                new LambdaUpdateWrapper<AiCardEntity>()
                        .eq(AiCardEntity::getId, entity.getId())
                        .eq(AiCardEntity::getVersion, version));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "卡片已被其他操作更新，请刷新后重试");
        }

        handleIconBinding(entity.getIconId(), null, entity.getId());

        log.info("delete ai card success cardId={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void batchSortCards(List<AiCardSortItemDTO> requestDTO) {
        if (requestDTO == null || requestDTO.isEmpty()) {
            log.warn("batch sort received empty list");
            return;
        }

        List<Long> requestedIds = requestDTO.stream().map(AiCardSortItemDTO::getId).toList();
        Set<Long> deduplicatedIds = new LinkedHashSet<>(requestedIds);
        if (deduplicatedIds.size() != requestedIds.size()) {
            log.warn("batch sort duplicate ids detected requestedIds={}", requestedIds);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复卡片");
        }

        List<AiCardEntity> activeCards = aiCardMapper.selectList(
                new LambdaQueryWrapper<AiCardEntity>()
                        .eq(AiCardEntity::getDeletedMarker, 0L)
                        .orderByAsc(AiCardEntity::getId));
        if (activeCards.isEmpty()) {
            log.warn("no active ai cards to sort");
            throw new BusinessException(ErrorCode.SITE_AI_CARD_NOT_FOUND, "暂无可排序的卡片");
        }

        Set<Long> currentIds = new LinkedHashSet<>(activeCards.stream().map(AiCardEntity::getId).toList());
        if (!deduplicatedIds.equals(currentIds)) {
            log.warn("batch sort completeness check failed requestedIds={} currentIds={}", deduplicatedIds, currentIds);
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前全部活跃卡片");
        }

        Map<Long, AiCardEntity> entityMap = new HashMap<>();
        for (AiCardEntity card : activeCards) {
            entityMap.put(card.getId(), card);
        }

        List<Map<String, Object>> before = activeCards.stream()
                .sorted(Comparator.comparing(AiCardEntity::getSortOrder).thenComparing(AiCardEntity::getId))
                .map(this::toSnapshot)
                .toList();

        for (AiCardSortItemDTO item : requestDTO) {
            AiCardEntity entity = entityMap.get(item.getId());
            if (entity == null) {
                log.error("batch sort entity not found in map cardId={}", item.getId());
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "卡片已被删除，请刷新后重试");
            }
            entity.setSortOrder(item.getSortOrder());
            ConcurrencyHelper.tryUpdate(aiCardMapper, entity);
        }

        List<Map<String, Object>> after = requestDTO.stream()
                .map(item -> entityMap.get(item.getId()))
                .map(this::toSnapshot)
                .toList();

        log.info("batch sort ai cards success count={}", requestDTO.size());
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalAiCardVO> getPortalCards() {
        String cacheKey = portalCacheSupport.buildKey(CACHE_SEGMENT);
        List<PortalAiCardVO> cached = portalCacheSupport.readListCache(cacheKey, PortalAiCardVO.class, CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        List<AiCardEntity> list = aiCardMapper.selectList(
                new LambdaQueryWrapper<AiCardEntity>()
                        .eq(AiCardEntity::getDeletedMarker, 0L)
                        .eq(AiCardEntity::getVisible, true)
                        .orderByAsc(AiCardEntity::getSortOrder)
                        .orderByAsc(AiCardEntity::getId));
        List<PortalAiCardVO> result = list.stream().map(aiCardConverter::toPortalVO).toList();

        portalCacheSupport.writeCache(cacheKey, result, portalCacheSupport.isEmptyResult(result), CACHE_SEGMENT);
        return result;
    }

    private AiCardEntity requireActiveCard(Long id) {
        AiCardEntity entity = aiCardMapper.selectOne(
                new LambdaQueryWrapper<AiCardEntity>()
                        .eq(AiCardEntity::getId, id)
                        .eq(AiCardEntity::getDeletedMarker, 0L));
        if (entity == null) {
            log.warn("ai card not found cardId={}", id);
            throw new BusinessException(ErrorCode.SITE_AI_CARD_NOT_FOUND);
        }
        return entity;
    }

    private void handleIconBinding(Long oldIconId, Long newIconId, Long bizObjectId) {
        mediaAssetService.bindMedia(newIconId, BIZ_MODULE, bizObjectId, MEDIA_BIZ_FIELD);
    }

    private int nextSortOrder() {
        AiCardEntity last = aiCardMapper.selectOne(
                new LambdaQueryWrapper<AiCardEntity>()
                        .eq(AiCardEntity::getDeletedMarker, 0L)
                        .orderByDesc(AiCardEntity::getSortOrder)
                        .orderByDesc(AiCardEntity::getId)
                        .last("limit 1"));
        int current = (last == null || last.getSortOrder() == null) ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值已达到上限，请先整理现有卡片");
        }
        return current + sortGap;
    }

    private Map<String, Object> toSnapshot(AiCardEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("name", entity.getName());
        snapshot.put("englishName", entity.getEnglishName());
        snapshot.put("iconId", entity.getIconId());
        snapshot.put("description", entity.getDescription());
        snapshot.put("jumpLink", entity.getJumpLink());
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
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
