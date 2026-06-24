package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.dto.HonorBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.HonorCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.HonorDeleteRequestDTO;
import com.company.officialwebsite.modules.site.dto.HonorUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.HonorEntity;
import com.company.officialwebsite.modules.site.mapper.HonorMapper;
import com.company.officialwebsite.modules.site.service.HonorService;
import com.company.officialwebsite.modules.site.vo.AdminHonorVO;
import com.company.officialwebsite.modules.site.vo.PortalHonorVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.Collection;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HonorServiceImpl：实现企业资质荣誉标签的后台维护、审计和前台缓存逻辑。
 */
@Service
public class HonorServiceImpl implements HonorService {

    private static final Logger log = LoggerFactory.getLogger(HonorServiceImpl.class);
    private static final String CACHE_SEGMENT = "honors";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "HONOR";
    private static final String ACTION_CREATE = "CREATE_HONOR";
    private static final String ACTION_UPDATE = "UPDATE_HONOR";
    private static final String ACTION_DELETE = "DELETE_HONOR";
    private static final String ACTION_REORDER = "REORDER_HONOR";
    private static final String MEDIA_BIZ_FIELD = "icon";
    private static final int SORT_GAP = 10;

    private final HonorMapper honorMapper;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheInvalidationSupport portalCacheInvalidationSupport;
    private final PortalCacheKeyBuilder portalCacheKeyBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OfficialProperties officialProperties;
    private final ObjectMapper objectMapper;

    public HonorServiceImpl(
            HonorMapper honorMapper,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            PortalCacheInvalidationSupport portalCacheInvalidationSupport,
            PortalCacheKeyBuilder portalCacheKeyBuilder,
            RedisTemplate<String, Object> redisTemplate,
            OfficialProperties officialProperties,
            ObjectMapper objectMapper) {
        this.honorMapper = honorMapper;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheInvalidationSupport = portalCacheInvalidationSupport;
        this.portalCacheKeyBuilder = portalCacheKeyBuilder;
        this.redisTemplate = redisTemplate;
        this.officialProperties = officialProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminHonorVO> getAdminHonors() {
        return listActiveHonors().stream().map(this::toAdminVO).toList();
    }

    @Override
    @Transactional
    public List<AdminHonorVO> createHonor(HonorCreateRequestDTO requestDTO) {
        HonorEntity entity = new HonorEntity();
        applyForCreate(entity, requestDTO);
        try {
            honorMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_HONOR_NAME_DUPLICATE);
        }
        mediaAssetService.bindMedia(entity.getIconId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        log.info("create honor success honorId={} visible={} sortOrder={}", entity.getId(), entity.getVisible(), entity.getSortOrder());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalHonors();
        return getAdminHonors();
    }

    @Override
    @Transactional
    public List<AdminHonorVO> updateHonor(Long honorId, HonorUpdateRequestDTO requestDTO) {
        HonorEntity entity = requireActiveHonor(honorId);
        assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        applyForUpdate(entity, requestDTO);
        try {
            tryUpdate(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_HONOR_NAME_DUPLICATE);
        }
        mediaAssetService.bindMedia(entity.getIconId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        log.info("update honor success honorId={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalHonors();
        return getAdminHonors();
    }

    @Override
    @Transactional
    public List<AdminHonorVO> deleteHonor(Long honorId, HonorDeleteRequestDTO requestDTO) {
        HonorEntity entity = requireActiveHonor(honorId);
        assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        int deleted = honorMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "荣誉已被其他操作更新，请刷新后重试");
        }
        mediaAssetService.bindMedia(null, BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        log.info("delete honor success honorId={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalHonors();
        return getAdminHonors();
    }

    @Override
    @Transactional
    public List<AdminHonorVO> reorderHonors(HonorBatchSortRequestDTO requestDTO) {
        List<HonorEntity> honors = listActiveHonors();
        if (honors.isEmpty()) {
            throw new BusinessException(ErrorCode.SITE_HONOR_NOT_FOUND, "排序目标不存在");
        }
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedHonorIds());
        if (requestedOrder.size() != requestDTO.getOrderedHonorIds().size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复荣誉");
        }
        Set<Long> currentIds = new LinkedHashSet<>(honors.stream().map(HonorEntity::getId).toList());
        if (!new LinkedHashSet<>(requestedOrder).equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前荣誉");
        }

        Map<Long, HonorEntity> entityMap = new HashMap<>();
        for (HonorEntity honor : honors) {
            entityMap.put(honor.getId(), honor);
        }
        List<Map<String, Object>> before = honors.stream().sorted(honorComparator()).map(this::toSnapshot).toList();
        for (int index = 0; index < requestedOrder.size(); index++) {
            HonorEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            tryUpdate(entity);
        }
        List<Map<String, Object>> after = requestedOrder.stream().map(entityMap::get).map(this::toSnapshot).toList();
        log.info("reorder honors success honorCount={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalHonors();
        return getAdminHonors();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalHonorVO> getPortalHonors() {
        String cacheKey = portalCacheKeyBuilder.build(CACHE_SEGMENT);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.convertValue(
                        cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, PortalHonorVO.class));
            }
        } catch (Exception ex) {
            log.warn("read portal honor cache failed key={}", cacheKey, ex);
        }

        List<PortalHonorVO> honors = listVisibleHonors().stream().map(this::toPortalVO).toList();
        try {
            Duration ttl = officialProperties.getCache().getDefaultTtl();
            redisTemplate.opsForValue().set(cacheKey, honors, ttl);
        } catch (Exception ex) {
            log.warn("write portal honor cache failed key={}", cacheKey, ex);
        }
        return honors;
    }

    private void applyForCreate(HonorEntity entity, HonorCreateRequestDTO requestDTO) {
        entity.setName(normalizeName(requestDTO.getName()));
        entity.setIconId(requireIcon(requestDTO.getIconId()).getId());
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(nextSortOrder());
        assertNameUnique(entity.getName(), null);
    }

    private void applyForUpdate(HonorEntity entity, HonorUpdateRequestDTO requestDTO) {
        entity.setName(normalizeName(requestDTO.getName()));
        entity.setIconId(requireIcon(requestDTO.getIconId()).getId());
        entity.setVisible(requestDTO.getVisible());
        assertNameUnique(entity.getName(), entity.getId());
    }

    private HonorEntity requireActiveHonor(Long honorId) {
        HonorEntity entity = honorMapper.selectOne(new LambdaQueryWrapper<HonorEntity>()
                .eq(HonorEntity::getId, honorId)
                .eq(HonorEntity::getDeletedMarker, 0L)
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.SITE_HONOR_NOT_FOUND);
        }
        return entity;
    }

    private List<HonorEntity> listActiveHonors() {
        return honorMapper.selectList(new LambdaQueryWrapper<HonorEntity>()
                .eq(HonorEntity::getDeletedMarker, 0L)
                .orderByAsc(HonorEntity::getSortOrder)
                .orderByAsc(HonorEntity::getId));
    }

    private List<HonorEntity> listVisibleHonors() {
        return honorMapper.selectList(new LambdaQueryWrapper<HonorEntity>()
                .eq(HonorEntity::getDeletedMarker, 0L)
                .eq(HonorEntity::getVisible, true)
                .orderByAsc(HonorEntity::getSortOrder)
                .orderByAsc(HonorEntity::getId));
    }

    private AdminHonorVO toAdminVO(HonorEntity entity) {
        MediaAssetEntity icon = requireIcon(entity.getIconId());
        AdminHonorVO vo = new AdminHonorVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setIconId(entity.getIconId());
        vo.setIconUrl(icon.getPublicUrl());
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private PortalHonorVO toPortalVO(HonorEntity entity) {
        MediaAssetEntity icon = requireIcon(entity.getIconId());
        PortalHonorVO vo = new PortalHonorVO();
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setIconUrl(icon.getPublicUrl());
        return vo;
    }

    private Map<String, Object> toSnapshot(HonorEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("name", entity.getName());
        snapshot.put("iconId", entity.getIconId());
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private MediaAssetEntity requireIcon(Long mediaId) {
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.SITE_HONOR_ICON_INVALID);
        }
    }

    private void assertNameUnique(String name, Long excludeId) {
        Long count = honorMapper.selectCount(new LambdaQueryWrapper<HonorEntity>()
                .eq(HonorEntity::getName, name)
                .eq(HonorEntity::getDeletedMarker, 0L)
                .ne(excludeId != null, HonorEntity::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.SITE_HONOR_NAME_DUPLICATE);
        }
    }

    private void tryUpdate(HonorEntity entity) {
        Integer requestVersion = entity.getVersion();
        int updated = honorMapper.updateById(entity);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "荣誉已被其他操作更新，请刷新后重试");
        }
        if (entity.getVersion() == null || entity.getVersion().equals(requestVersion)) {
            entity.setVersion(requestVersion + 1);
        }
    }

    private void assertVersion(Integer currentVersion, Integer requestVersion) {
        if (requestVersion == null || requestVersion < 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "版本号不能为负数");
        }
        if (!Objects.equals(currentVersion, requestVersion)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "荣誉已被其他操作更新，请刷新后重试");
        }
    }

    private List<Long> deduplicateIds(Collection<Long> orderedHonorIds) {
        if (orderedHonorIds == null) {
            return List.of();
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long orderedHonorId : orderedHonorIds) {
            if (orderedHonorId == null) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序荣誉 ID 不能为空");
            }
            deduplicated.add(orderedHonorId);
        }
        return List.copyOf(deduplicated);
    }

    private String normalizeName(String name) {
        String normalized = StringFieldUtils.trimToNull(name);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "荣誉名称不能为空");
        }
        return normalized;
    }

    private int nextSortOrder() {
        HonorEntity last = honorMapper.selectOne(new LambdaQueryWrapper<HonorEntity>()
                .eq(HonorEntity::getDeletedMarker, 0L)
                .orderByDesc(HonorEntity::getSortOrder)
                .orderByDesc(HonorEntity::getId)
                .last("limit 1"));
        int current = last == null || last.getSortOrder() == null ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - SORT_GAP) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "荣誉排序值已达到上限");
        }
        return current + SORT_GAP;
    }

    private int sortOrderForIndex(int index) {
        try {
            return Math.multiplyExact(Math.addExact(index, 1), SORT_GAP);
        } catch (ArithmeticException ex) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值超出允许范围");
        }
    }

    private Comparator<HonorEntity> honorComparator() {
        return Comparator
                .comparing(HonorEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(HonorEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private void invalidatePortalHonors() {
        portalCacheInvalidationSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
