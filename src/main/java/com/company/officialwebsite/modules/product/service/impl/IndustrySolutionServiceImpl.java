package com.company.officialwebsite.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.product.converter.IndustrySolutionConverter;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionBatchSortDTO;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionCreateDTO;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionDeleteDTO;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionUpdateDTO;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionEntity;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionMapper;
import com.company.officialwebsite.modules.product.service.IndustrySolutionService;
import com.company.officialwebsite.modules.product.vo.AdminIndustrySolutionVO;
import com.company.officialwebsite.modules.product.vo.PortalIndustrySolutionVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
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
 * IndustrySolutionServiceImpl：实现行业解决方案的 CRUD、排序、媒体生命周期联动、审计和 Portal 缓存逻辑。
 */
@Service
public class IndustrySolutionServiceImpl implements IndustrySolutionService {

    private static final Logger log = LoggerFactory.getLogger(IndustrySolutionServiceImpl.class);

    private static final String BIZ_MODULE = "PRODUCT";
    private static final String TARGET_TYPE = "INDUSTRY_SOLUTION";
    private static final String CACHE_SEGMENT = "industry_solutions";
    private static final String MEDIA_BIZ_FIELD = "icon";
    private static final String ACTION_CREATE = "CREATE_SOLUTION";
    private static final String ACTION_UPDATE = "UPDATE_SOLUTION";
    private static final String ACTION_DELETE = "DELETE_SOLUTION";
    private static final String ACTION_REORDER = "REORDER_SOLUTION";

    private final IndustrySolutionMapper industrySolutionMapper;
    private final IndustrySolutionConverter industrySolutionConverter;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheInvalidationSupport portalCacheInvalidationSupport;
    private final PortalCacheKeyBuilder portalCacheKeyBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OfficialProperties officialProperties;
    private final ObjectMapper objectMapper;
    private final int sortGap;

    public IndustrySolutionServiceImpl(
            IndustrySolutionMapper industrySolutionMapper,
            IndustrySolutionConverter industrySolutionConverter,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            PortalCacheInvalidationSupport portalCacheInvalidationSupport,
            PortalCacheKeyBuilder portalCacheKeyBuilder,
            RedisTemplate<String, Object> redisTemplate,
            OfficialProperties officialProperties,
            ObjectMapper objectMapper) {
        this.industrySolutionMapper = industrySolutionMapper;
        this.industrySolutionConverter = industrySolutionConverter;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheInvalidationSupport = portalCacheInvalidationSupport;
        this.portalCacheKeyBuilder = portalCacheKeyBuilder;
        this.redisTemplate = redisTemplate;
        this.officialProperties = officialProperties;
        this.objectMapper = objectMapper;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminIndustrySolutionVO> getAdminIndustrySolutionList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);
        Page<IndustrySolutionEntity> page = industrySolutionMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<IndustrySolutionEntity>()
                        .eq(IndustrySolutionEntity::getDeletedMarker, 0L)
                        .orderByAsc(IndustrySolutionEntity::getSortOrder)
                        .orderByAsc(IndustrySolutionEntity::getId));
        List<AdminIndustrySolutionVO> list = page.getRecords().stream().map(industrySolutionConverter::toAdminVO).toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public List<AdminIndustrySolutionVO> createIndustrySolution(IndustrySolutionCreateDTO createDTO) {
        IndustrySolutionEntity entity = new IndustrySolutionEntity();
        entity.setName(normalizeRequiredText(createDTO.getName(), 100, "行业名称"));
        entity.setIconMediaId(validateAndResolveIcon(createDTO.getIconMediaId()));
        entity.setDescription(normalizeRequiredText(createDTO.getDescription(), 500, "行业方案描述"));
        entity.setCustomerTags(normalizeCustomerTags(createDTO.getCustomerTags()));
        entity.setVisible(createDTO.getVisible());
        entity.setSortOrder(nextSortOrder());

        try {
            industrySolutionMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create industry solution duplicate name name={}", entity.getName());
            throw new BusinessException(ErrorCode.PRODUCT_SOLUTION_NAME_DUPLICATE);
        }

        mediaAssetService.bindMedia(entity.getIconMediaId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        log.info("create industry solution success solutionId={} name={} sortOrder={}", entity.getId(), entity.getName(), entity.getSortOrder());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
        return getAdminSolutionVOList();
    }

    @Override
    @Transactional
    public List<AdminIndustrySolutionVO> updateIndustrySolution(Long id, IndustrySolutionUpdateDTO updateDTO) {
        IndustrySolutionEntity entity = requireActiveIndustrySolution(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), updateDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        Long oldIconMediaId = entity.getIconMediaId();

        entity.setName(normalizeRequiredText(updateDTO.getName(), 100, "行业名称"));
        entity.setIconMediaId(validateAndResolveIcon(updateDTO.getIconMediaId()));
        entity.setDescription(normalizeRequiredText(updateDTO.getDescription(), 500, "行业方案描述"));
        entity.setCustomerTags(normalizeCustomerTags(updateDTO.getCustomerTags()));
        entity.setVisible(updateDTO.getVisible());

        try {
            ConcurrencyHelper.tryUpdate(industrySolutionMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update industry solution duplicate name solutionId={} name={}", entity.getId(), entity.getName());
            throw new BusinessException(ErrorCode.PRODUCT_SOLUTION_NAME_DUPLICATE);
        }

        if (!Objects.equals(oldIconMediaId, entity.getIconMediaId())) {
            mediaAssetService.bindMedia(entity.getIconMediaId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        }

        log.info("update industry solution success solutionId={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
        return getAdminSolutionVOList();
    }

    @Override
    @Transactional
    public List<AdminIndustrySolutionVO> deleteIndustrySolution(Long id, IndustrySolutionDeleteDTO deleteDTO) {
        IndustrySolutionEntity entity = requireActiveIndustrySolution(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), deleteDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);

        int deleted = industrySolutionMapper.delete(
                new LambdaUpdateWrapper<IndustrySolutionEntity>()
                        .eq(IndustrySolutionEntity::getId, entity.getId())
                        .eq(IndustrySolutionEntity::getVersion, deleteDTO.getVersion()));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }

        mediaAssetService.bindMedia(null, BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);

        log.info("delete industry solution success solutionId={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
        return getAdminSolutionVOList();
    }

    @Override
    @Transactional
    public List<AdminIndustrySolutionVO> batchSortIndustrySolutions(IndustrySolutionBatchSortDTO sortDTO) {
        List<Long> orderedIds = sortDTO.getOrderedIds();
        if (orderedIds == null || orderedIds.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能为空");
        }

        Set<Long> deduplicatedIds = new LinkedHashSet<>(orderedIds);
        if (deduplicatedIds.size() != orderedIds.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复的行业解决方案");
        }

        List<IndustrySolutionEntity> activeSolutions = industrySolutionMapper.selectList(
                new LambdaQueryWrapper<IndustrySolutionEntity>()
                        .eq(IndustrySolutionEntity::getDeletedMarker, 0L)
                        .orderByAsc(IndustrySolutionEntity::getId));
        if (activeSolutions.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_SOLUTION_NOT_FOUND, "暂无可排序的行业解决方案");
        }

        Set<Long> currentIds = new LinkedHashSet<>(activeSolutions.stream().map(IndustrySolutionEntity::getId).toList());
        if (!deduplicatedIds.equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前全部活跃行业解决方案");
        }

        Map<Long, IndustrySolutionEntity> entityMap = new HashMap<>();
        for (IndustrySolutionEntity entity : activeSolutions) {
            entityMap.put(entity.getId(), entity);
        }

        List<Map<String, Object>> before = activeSolutions.stream()
                .sorted(Comparator.comparing(IndustrySolutionEntity::getSortOrder).thenComparing(IndustrySolutionEntity::getId))
                .map(this::toSnapshot)
                .toList();

        int orderIndex = 1;
        for (Long solutionId : orderedIds) {
            IndustrySolutionEntity entity = entityMap.get(solutionId);
            if (entity == null) {
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "行业解决方案已被删除，请刷新后重试");
            }
            entity.setSortOrder(orderIndex * sortGap);
            ConcurrencyHelper.tryUpdate(industrySolutionMapper, entity);
            orderIndex++;
        }

        List<Map<String, Object>> after = orderedIds.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();

        log.info("batch sort industry solutions success count={}", orderedIds.size());
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
        return getAdminSolutionVOList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalIndustrySolutionVO> getPortalIndustrySolutions() {
        String cacheKey = portalCacheKeyBuilder.build(CACHE_SEGMENT);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.convertValue(
                        cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, PortalIndustrySolutionVO.class));
            }
        } catch (Exception ex) {
            log.warn("read portal industry solutions cache failed key={}", cacheKey, ex);
        }

        List<IndustrySolutionEntity> list = industrySolutionMapper.selectList(
                new LambdaQueryWrapper<IndustrySolutionEntity>()
                        .eq(IndustrySolutionEntity::getDeletedMarker, 0L)
                        .eq(IndustrySolutionEntity::getVisible, true)
                        .orderByAsc(IndustrySolutionEntity::getSortOrder)
                        .orderByAsc(IndustrySolutionEntity::getId));
        List<PortalIndustrySolutionVO> result = list.stream().map(industrySolutionConverter::toPortalVO).toList();

        try {
            Duration ttl = officialProperties.getCache().getDefaultTtl();
            redisTemplate.opsForValue().set(cacheKey, result, ttl);
        } catch (Exception ex) {
            log.warn("write portal industry solutions cache failed key={}", cacheKey, ex);
        }
        return result;
    }

    private List<AdminIndustrySolutionVO> getAdminSolutionVOList() {
        List<IndustrySolutionEntity> list = industrySolutionMapper.selectList(
                new LambdaQueryWrapper<IndustrySolutionEntity>()
                        .eq(IndustrySolutionEntity::getDeletedMarker, 0L)
                        .orderByAsc(IndustrySolutionEntity::getSortOrder)
                        .orderByAsc(IndustrySolutionEntity::getId));
        return list.stream().map(industrySolutionConverter::toAdminVO).toList();
    }

    private IndustrySolutionEntity requireActiveIndustrySolution(Long id) {
        IndustrySolutionEntity entity = industrySolutionMapper.selectOne(
                new LambdaQueryWrapper<IndustrySolutionEntity>()
                        .eq(IndustrySolutionEntity::getId, id)
                        .eq(IndustrySolutionEntity::getDeletedMarker, 0L));
        if (entity == null) {
            log.warn("industry solution not found or deleted solutionId={}", id);
            throw new BusinessException(ErrorCode.PRODUCT_SOLUTION_NOT_FOUND);
        }
        return entity;
    }

    private Long validateAndResolveIcon(Long iconMediaId) {
        if (iconMediaId == null) {
            log.warn("industry solution icon media id missing");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "行业解决方案图标不能为空");
        }
        try {
            mediaAssetService.requireUsableImage(iconMediaId);
            return iconMediaId;
        } catch (BusinessException ex) {
            log.warn("industry solution icon invalid mediaId={} errorCode={}", iconMediaId, ex.getErrorCode().getCode());
            throw new BusinessException(ErrorCode.PRODUCT_SOLUTION_ICON_INVALID);
        }
    }

    private List<String> normalizeCustomerTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        if (tags.size() > 10) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "典型客户标签最多 10 个");
        }
        List<String> normalized = new ArrayList<>();
        for (String tag : tags) {
            String normalizedTag = StringFieldUtils.trimToNull(tag);
            if (normalizedTag == null) {
                log.warn("industry solution customer tag invalid due to blank value");
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "客户标签不能为空");
            }
            if (normalizedTag.length() > 30) {
                log.warn("industry solution customer tag too long length={}", normalizedTag.length());
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "客户标签最长 30 字符");
            }
            if (normalized.contains(normalizedTag)) {
                log.warn("industry solution customer tag duplicated tag={}", normalizedTag);
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "客户标签不能重复");
            }
            normalized.add(normalizedTag);
        }
        return normalized;
    }

    private String normalizeRequiredText(String value, int maxLength, String fieldName) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            log.warn("industry solution required text missing field={}", fieldName);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, fieldName + "不能为空");
        }
        if (normalized.length() > maxLength) {
            log.warn("industry solution required text too long field={} length={} maxLength={}", fieldName, normalized.length(), maxLength);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, fieldName + "最长 " + maxLength + " 字符");
        }
        return normalized;
    }

    private int nextSortOrder() {
        IndustrySolutionEntity last = industrySolutionMapper.selectOne(
                new LambdaQueryWrapper<IndustrySolutionEntity>()
                        .eq(IndustrySolutionEntity::getDeletedMarker, 0L)
                        .orderByDesc(IndustrySolutionEntity::getSortOrder)
                        .orderByDesc(IndustrySolutionEntity::getId)
                        .last("limit 1"));
        int current = (last == null || last.getSortOrder() == null) ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值已达到上限，请先整理现有行业解决方案");
        }
        return current + sortGap;
    }

    private Map<String, Object> toSnapshot(IndustrySolutionEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("name", entity.getName());
        snapshot.put("iconMediaId", entity.getIconMediaId());
        snapshot.put("description", entity.getDescription());
        snapshot.put("customerTags", entity.getCustomerTags());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    /**
     * 行业解决方案写操作必须在事务提交后再失效 Portal 缓存，避免回滚时污染前台读缓存。
     */
    private void invalidatePortalCache() {
        portalCacheInvalidationSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
