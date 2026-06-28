package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.converter.ResearchDirectionConverter;
import com.company.officialwebsite.modules.site.dto.ResearchDirectionBatchSortDTO;
import com.company.officialwebsite.modules.site.dto.ResearchDirectionCreateDTO;
import com.company.officialwebsite.modules.site.dto.ResearchDirectionUpdateDTO;
import com.company.officialwebsite.modules.site.entity.ResearchDirectionEntity;
import com.company.officialwebsite.modules.site.mapper.ResearchDirectionMapper;
import com.company.officialwebsite.modules.site.service.ResearchDirectionService;
import com.company.officialwebsite.modules.site.vo.AdminResearchDirectionVO;
import com.company.officialwebsite.modules.site.vo.PortalResearchDirectionVO;
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
 * ResearchDirectionServiceImpl：实现研发方向管理、媒体绑定、审计和缓存失效逻辑。
 */
@Service
public class ResearchDirectionServiceImpl implements ResearchDirectionService {

    private static final Logger log = LoggerFactory.getLogger(ResearchDirectionServiceImpl.class);
    private static final String CACHE_SEGMENT = "research_directions";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "RESEARCH_DIRECTION";
    private static final String ACTION_CREATE = "CREATE_RESEARCH";
    private static final String ACTION_UPDATE = "UPDATE_RESEARCH";
    private static final String ACTION_DELETE = "DELETE_RESEARCH";
    private static final String ACTION_REORDER = "REORDER_RESEARCH";
    private static final String MEDIA_BIZ_FIELD = "icon";

    private final ResearchDirectionMapper researchDirectionMapper;
    private final ResearchDirectionConverter researchDirectionConverter;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final int sortGap;

    public ResearchDirectionServiceImpl(
            ResearchDirectionMapper researchDirectionMapper,
            ResearchDirectionConverter researchDirectionConverter,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport) {
        this.researchDirectionMapper = researchDirectionMapper;
        this.researchDirectionConverter = researchDirectionConverter;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminResearchDirectionVO> getAdminDirections() {
        return listActiveDirections().stream().map(researchDirectionConverter::toAdminVO).toList();
    }

    @Override
    @Transactional
    public List<AdminResearchDirectionVO> createDirection(ResearchDirectionCreateDTO requestDTO) {
        ResearchDirectionEntity entity = new ResearchDirectionEntity();
        applyForCreate(entity, requestDTO);
        try {
            researchDirectionMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_RESEARCH_DIRECTION_TITLE_DUPLICATE);
        }
        bindIcon(entity.getIconMediaId(), entity.getId());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
        return getAdminDirections();
    }

    @Override
    @Transactional
    public List<AdminResearchDirectionVO> updateDirection(Long id, ResearchDirectionUpdateDTO requestDTO) {
        ResearchDirectionEntity entity = requireActiveDirection(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        Long oldIconMediaId = entity.getIconMediaId();

        applyForUpdate(entity, requestDTO);
        try {
            ConcurrencyHelper.tryUpdate(researchDirectionMapper, entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_RESEARCH_DIRECTION_TITLE_DUPLICATE);
        }

        if (!Objects.equals(oldIconMediaId, entity.getIconMediaId())) {
            bindIcon(entity.getIconMediaId(), entity.getId());
        }

        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
        return getAdminDirections();
    }

    @Override
    @Transactional
    public List<AdminResearchDirectionVO> deleteDirection(Long id, Integer version) {
        ResearchDirectionEntity entity = requireActiveDirection(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);

        int deleted = researchDirectionMapper.delete(
                new LambdaUpdateWrapper<ResearchDirectionEntity>()
                        .eq(ResearchDirectionEntity::getId, entity.getId())
                        .eq(ResearchDirectionEntity::getVersion, version));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }

        mediaAssetService.bindMedia(null, BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
        return getAdminDirections();
    }

    @Override
    @Transactional
    public List<AdminResearchDirectionVO> batchSortDirections(ResearchDirectionBatchSortDTO requestDTO) {
        List<Long> orderedIds = requestDTO.getOrderedIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能为空");
        }
        Set<Long> deduplicatedIds = new LinkedHashSet<>(orderedIds);
        if (deduplicatedIds.size() != orderedIds.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复研发方向");
        }

        List<ResearchDirectionEntity> activeDirections = listActiveDirections();
        if (activeDirections.isEmpty()) {
            throw new BusinessException(ErrorCode.SITE_RESEARCH_DIRECTION_NOT_FOUND, "暂无可排序的研发方向");
        }

        Set<Long> currentIds = new LinkedHashSet<>(activeDirections.stream().map(ResearchDirectionEntity::getId).toList());
        if (!deduplicatedIds.equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前全部活跃研发方向");
        }

        Map<Long, ResearchDirectionEntity> entityMap = new HashMap<>();
        for (ResearchDirectionEntity entity : activeDirections) {
            entityMap.put(entity.getId(), entity);
        }

        List<Map<String, Object>> before = activeDirections.stream()
                .sorted(Comparator.comparing(ResearchDirectionEntity::getSortOrder).thenComparing(ResearchDirectionEntity::getId))
                .map(this::toSnapshot)
                .toList();

        int orderIndex = 1;
        for (Long directionId : orderedIds) {
            ResearchDirectionEntity entity = entityMap.get(directionId);
            if (entity == null) {
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "研发方向已被删除，请刷新后重试");
            }
            entity.setSortOrder(orderIndex * sortGap);
            ConcurrencyHelper.tryUpdate(researchDirectionMapper, entity);
            orderIndex++;
        }

        List<Map<String, Object>> after = orderedIds.stream().map(entityMap::get).map(this::toSnapshot).toList();
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
        return getAdminDirections();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalResearchDirectionVO> getPortalDirections() {
        String cacheKey = portalCacheSupport.buildKey(CACHE_SEGMENT);
        List<PortalResearchDirectionVO> cached = portalCacheSupport.readListCache(cacheKey, PortalResearchDirectionVO.class, CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        List<PortalResearchDirectionVO> result = listVisibleDirections().stream().map(researchDirectionConverter::toPortalVO).toList();
        portalCacheSupport.writeCache(cacheKey, result, portalCacheSupport.isEmptyResult(result), CACHE_SEGMENT);
        return result;
    }

    private void applyForCreate(ResearchDirectionEntity entity, ResearchDirectionCreateDTO requestDTO) {
        entity.setTitleCn(normalizeRequiredText(requestDTO.getTitleCn(), 100, "中文标题"));
        entity.setTitleEn(normalizeRequiredText(requestDTO.getTitleEn(), 100, "英文标题"));
        entity.setSummary(normalizeRequiredText(requestDTO.getSummary(), 512, "研发方向描述"));
        entity.setIconMediaId(requireIcon(requestDTO.getIconMediaId()).getId());
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(nextSortOrder());
        assertTitleUnique(entity.getTitleCn(), null);
    }

    private void applyForUpdate(ResearchDirectionEntity entity, ResearchDirectionUpdateDTO requestDTO) {
        entity.setTitleCn(normalizeRequiredText(requestDTO.getTitleCn(), 100, "中文标题"));
        entity.setTitleEn(normalizeRequiredText(requestDTO.getTitleEn(), 100, "英文标题"));
        entity.setSummary(normalizeRequiredText(requestDTO.getSummary(), 512, "研发方向描述"));
        entity.setIconMediaId(requireIcon(requestDTO.getIconMediaId()).getId());
        entity.setVisible(requestDTO.getVisible());
        assertTitleUnique(entity.getTitleCn(), entity.getId());
    }

    private ResearchDirectionEntity requireActiveDirection(Long id) {
        ResearchDirectionEntity entity = researchDirectionMapper.selectOne(
                new LambdaQueryWrapper<ResearchDirectionEntity>()
                        .eq(ResearchDirectionEntity::getId, id)
                        .eq(ResearchDirectionEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.SITE_RESEARCH_DIRECTION_NOT_FOUND);
        }
        return entity;
    }

    private List<ResearchDirectionEntity> listActiveDirections() {
        return researchDirectionMapper.selectList(
                new LambdaQueryWrapper<ResearchDirectionEntity>()
                        .eq(ResearchDirectionEntity::getDeletedMarker, 0L)
                        .orderByAsc(ResearchDirectionEntity::getSortOrder)
                        .orderByAsc(ResearchDirectionEntity::getId));
    }

    private List<ResearchDirectionEntity> listVisibleDirections() {
        return researchDirectionMapper.selectList(
                new LambdaQueryWrapper<ResearchDirectionEntity>()
                        .eq(ResearchDirectionEntity::getDeletedMarker, 0L)
                        .eq(ResearchDirectionEntity::getVisible, true)
                        .orderByAsc(ResearchDirectionEntity::getSortOrder)
                        .orderByAsc(ResearchDirectionEntity::getId));
    }

    private MediaAssetEntity requireIcon(Long mediaId) {
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.SITE_RESEARCH_DIRECTION_ICON_INVALID);
        }
    }

    private void bindIcon(Long mediaId, Long directionId) {
        mediaAssetService.bindMedia(mediaId, BIZ_MODULE, directionId, MEDIA_BIZ_FIELD);
    }

    private String normalizeRequiredText(String value, int maxLength, String fieldName) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, fieldName + "不能为空");
        }
        if (normalized.length() > maxLength) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, fieldName + "最长 " + maxLength + " 字符");
        }
        return normalized;
    }

    private void assertTitleUnique(String titleCn, Long excludeId) {
        Long count = researchDirectionMapper.selectCount(new LambdaQueryWrapper<ResearchDirectionEntity>()
                .eq(ResearchDirectionEntity::getTitleCn, titleCn)
                .eq(ResearchDirectionEntity::getDeletedMarker, 0L)
                .ne(excludeId != null, ResearchDirectionEntity::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.SITE_RESEARCH_DIRECTION_TITLE_DUPLICATE);
        }
    }

    private int nextSortOrder() {
        ResearchDirectionEntity last = researchDirectionMapper.selectOne(
                new LambdaQueryWrapper<ResearchDirectionEntity>()
                        .eq(ResearchDirectionEntity::getDeletedMarker, 0L)
                        .orderByDesc(ResearchDirectionEntity::getSortOrder)
                        .orderByDesc(ResearchDirectionEntity::getId)
                        .last("limit 1"));
        int current = (last == null || last.getSortOrder() == null) ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值已达到上限，请先整理现有研发方向");
        }
        return current + sortGap;
    }

    private Map<String, Object> toSnapshot(ResearchDirectionEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("titleCn", entity.getTitleCn());
        snapshot.put("titleEn", entity.getTitleEn());
        snapshot.put("summary", entity.getSummary());
        snapshot.put("iconMediaId", entity.getIconMediaId());
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
