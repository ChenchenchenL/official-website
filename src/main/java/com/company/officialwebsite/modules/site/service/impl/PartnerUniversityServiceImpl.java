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
import com.company.officialwebsite.modules.site.converter.PartnerUniversityConverter;
import com.company.officialwebsite.modules.site.dto.PartnerUniversityBatchSortDTO;
import com.company.officialwebsite.modules.site.dto.PartnerUniversityCreateDTO;
import com.company.officialwebsite.modules.site.dto.PartnerUniversityUpdateDTO;
import com.company.officialwebsite.modules.site.entity.PartnerUniversityEntity;
import com.company.officialwebsite.modules.site.mapper.PartnerUniversityMapper;
import com.company.officialwebsite.modules.site.service.PartnerUniversityService;
import com.company.officialwebsite.modules.site.vo.AdminPartnerUniversityVO;
import com.company.officialwebsite.modules.site.vo.PortalPartnerUniversityVO;
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
 * PartnerUniversityServiceImpl：实现合作高校管理、媒体绑定、审计和缓存失效逻辑。
 */
@Service
public class PartnerUniversityServiceImpl implements PartnerUniversityService {

    private static final Logger log = LoggerFactory.getLogger(PartnerUniversityServiceImpl.class);
    private static final String CACHE_SEGMENT = "partner_universities";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "PARTNER_UNIVERSITY";
    private static final String ACTION_CREATE = "CREATE_UNIVERSITY";
    private static final String ACTION_UPDATE = "UPDATE_UNIVERSITY";
    private static final String ACTION_DELETE = "DELETE_UNIVERSITY";
    private static final String ACTION_REORDER = "REORDER_UNIVERSITY";
    private static final String MEDIA_BIZ_FIELD = "logo";

    private final PartnerUniversityMapper partnerUniversityMapper;
    private final PartnerUniversityConverter partnerUniversityConverter;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final int sortGap;

    public PartnerUniversityServiceImpl(
            PartnerUniversityMapper partnerUniversityMapper,
            PartnerUniversityConverter partnerUniversityConverter,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport) {
        this.partnerUniversityMapper = partnerUniversityMapper;
        this.partnerUniversityConverter = partnerUniversityConverter;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminPartnerUniversityVO> getAdminUniversities() {
        return listActiveUniversities().stream().map(partnerUniversityConverter::toAdminVO).toList();
    }

    @Override
    @Transactional
    public List<AdminPartnerUniversityVO> createUniversity(PartnerUniversityCreateDTO requestDTO) {
        PartnerUniversityEntity entity = new PartnerUniversityEntity();
        applyForCreate(entity, requestDTO);
        try {
            partnerUniversityMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_UNIVERSITY_NAME_DUPLICATE);
        }
        bindLogo(entity.getLogoMediaId(), entity.getId());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
        return getAdminUniversities();
    }

    @Override
    @Transactional
    public List<AdminPartnerUniversityVO> updateUniversity(Long id, PartnerUniversityUpdateDTO requestDTO) {
        PartnerUniversityEntity entity = requireActiveUniversity(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        Long oldLogoMediaId = entity.getLogoMediaId();

        applyForUpdate(entity, requestDTO);
        try {
            ConcurrencyHelper.tryUpdate(partnerUniversityMapper, entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_UNIVERSITY_NAME_DUPLICATE);
        }

        if (!Objects.equals(oldLogoMediaId, entity.getLogoMediaId())) {
            bindLogo(entity.getLogoMediaId(), entity.getId());
        }

        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
        return getAdminUniversities();
    }

    @Override
    @Transactional
    public List<AdminPartnerUniversityVO> deleteUniversity(Long id, Integer version) {
        PartnerUniversityEntity entity = requireActiveUniversity(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);

        int deleted = partnerUniversityMapper.delete(
                new LambdaUpdateWrapper<PartnerUniversityEntity>()
                        .eq(PartnerUniversityEntity::getId, entity.getId())
                        .eq(PartnerUniversityEntity::getVersion, version));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }

        mediaAssetService.bindMedia(null, BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
        return getAdminUniversities();
    }

    @Override
    @Transactional
    public List<AdminPartnerUniversityVO> batchSortUniversities(PartnerUniversityBatchSortDTO requestDTO) {
        List<Long> orderedIds = requestDTO.getOrderedIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能为空");
        }
        Set<Long> deduplicatedIds = new LinkedHashSet<>(orderedIds);
        if (deduplicatedIds.size() != orderedIds.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复高校");
        }

        List<PartnerUniversityEntity> activeUniversities = listActiveUniversities();
        if (activeUniversities.isEmpty()) {
            throw new BusinessException(ErrorCode.SITE_UNIVERSITY_NOT_FOUND, "暂无可排序的合作高校");
        }

        Set<Long> currentIds = new LinkedHashSet<>(activeUniversities.stream().map(PartnerUniversityEntity::getId).toList());
        if (!deduplicatedIds.equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前全部活跃高校");
        }

        Map<Long, PartnerUniversityEntity> entityMap = new HashMap<>();
        for (PartnerUniversityEntity entity : activeUniversities) {
            entityMap.put(entity.getId(), entity);
        }

        List<Map<String, Object>> before = activeUniversities.stream()
                .sorted(Comparator.comparing(PartnerUniversityEntity::getSortOrder).thenComparing(PartnerUniversityEntity::getId))
                .map(this::toSnapshot)
                .toList();

        int orderIndex = 1;
        for (Long universityId : orderedIds) {
            PartnerUniversityEntity entity = entityMap.get(universityId);
            if (entity == null) {
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "合作高校已被删除，请刷新后重试");
            }
            entity.setSortOrder(orderIndex * sortGap);
            ConcurrencyHelper.tryUpdate(partnerUniversityMapper, entity);
            orderIndex++;
        }

        List<Map<String, Object>> after = orderedIds.stream().map(entityMap::get).map(this::toSnapshot).toList();
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
        return getAdminUniversities();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalPartnerUniversityVO> getPortalUniversities() {
        String cacheKey = portalCacheSupport.buildKey(CACHE_SEGMENT);
        List<PortalPartnerUniversityVO> cached = portalCacheSupport.readListCache(cacheKey, PortalPartnerUniversityVO.class, CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        List<PortalPartnerUniversityVO> result = listVisibleUniversities().stream().map(partnerUniversityConverter::toPortalVO).toList();
        portalCacheSupport.writeCache(cacheKey, result, portalCacheSupport.isEmptyResult(result), CACHE_SEGMENT);
        return result;
    }

    private void applyForCreate(PartnerUniversityEntity entity, PartnerUniversityCreateDTO requestDTO) {
        entity.setName(normalizeRequiredText(requestDTO.getName(), 100, "高校简称"));
        entity.setFullName(normalizeRequiredText(requestDTO.getFullName(), 200, "高校全称"));
        entity.setLogoMediaId(requireLogo(requestDTO.getLogoMediaId()).getId());
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(nextSortOrder());
        assertUnique(entity.getName(), entity.getFullName(), null);
    }

    private void applyForUpdate(PartnerUniversityEntity entity, PartnerUniversityUpdateDTO requestDTO) {
        entity.setName(normalizeRequiredText(requestDTO.getName(), 100, "高校简称"));
        entity.setFullName(normalizeRequiredText(requestDTO.getFullName(), 200, "高校全称"));
        entity.setLogoMediaId(requireLogo(requestDTO.getLogoMediaId()).getId());
        entity.setVisible(requestDTO.getVisible());
        assertUnique(entity.getName(), entity.getFullName(), entity.getId());
    }

    private PartnerUniversityEntity requireActiveUniversity(Long id) {
        PartnerUniversityEntity entity = partnerUniversityMapper.selectOne(
                new LambdaQueryWrapper<PartnerUniversityEntity>()
                        .eq(PartnerUniversityEntity::getId, id)
                        .eq(PartnerUniversityEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.SITE_UNIVERSITY_NOT_FOUND);
        }
        return entity;
    }

    private List<PartnerUniversityEntity> listActiveUniversities() {
        return partnerUniversityMapper.selectList(
                new LambdaQueryWrapper<PartnerUniversityEntity>()
                        .eq(PartnerUniversityEntity::getDeletedMarker, 0L)
                        .orderByAsc(PartnerUniversityEntity::getSortOrder)
                        .orderByAsc(PartnerUniversityEntity::getId));
    }

    private List<PartnerUniversityEntity> listVisibleUniversities() {
        return partnerUniversityMapper.selectList(
                new LambdaQueryWrapper<PartnerUniversityEntity>()
                        .eq(PartnerUniversityEntity::getDeletedMarker, 0L)
                        .eq(PartnerUniversityEntity::getVisible, true)
                        .orderByAsc(PartnerUniversityEntity::getSortOrder)
                        .orderByAsc(PartnerUniversityEntity::getId));
    }

    private MediaAssetEntity requireLogo(Long mediaId) {
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.SITE_UNIVERSITY_LOGO_INVALID);
        }
    }

    private void bindLogo(Long mediaId, Long universityId) {
        mediaAssetService.bindMedia(mediaId, BIZ_MODULE, universityId, MEDIA_BIZ_FIELD);
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

    private void assertUnique(String name, String fullName, Long excludeId) {
        Long duplicateNameCount = partnerUniversityMapper.selectCount(new LambdaQueryWrapper<PartnerUniversityEntity>()
                .eq(PartnerUniversityEntity::getName, name)
                .eq(PartnerUniversityEntity::getDeletedMarker, 0L)
                .ne(excludeId != null, PartnerUniversityEntity::getId, excludeId));
        Long duplicateFullNameCount = partnerUniversityMapper.selectCount(new LambdaQueryWrapper<PartnerUniversityEntity>()
                .eq(PartnerUniversityEntity::getFullName, fullName)
                .eq(PartnerUniversityEntity::getDeletedMarker, 0L)
                .ne(excludeId != null, PartnerUniversityEntity::getId, excludeId));
        if ((duplicateNameCount != null && duplicateNameCount > 0) || (duplicateFullNameCount != null && duplicateFullNameCount > 0)) {
            throw new BusinessException(ErrorCode.SITE_UNIVERSITY_NAME_DUPLICATE);
        }
    }

    private int nextSortOrder() {
        PartnerUniversityEntity last = partnerUniversityMapper.selectOne(
                new LambdaQueryWrapper<PartnerUniversityEntity>()
                        .eq(PartnerUniversityEntity::getDeletedMarker, 0L)
                        .orderByDesc(PartnerUniversityEntity::getSortOrder)
                        .orderByDesc(PartnerUniversityEntity::getId)
                        .last("limit 1"));
        int current = (last == null || last.getSortOrder() == null) ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值已达到上限，请先整理现有高校");
        }
        return current + sortGap;
    }

    private Map<String, Object> toSnapshot(PartnerUniversityEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("name", entity.getName());
        snapshot.put("fullName", entity.getFullName());
        snapshot.put("logoMediaId", entity.getLogoMediaId());
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
