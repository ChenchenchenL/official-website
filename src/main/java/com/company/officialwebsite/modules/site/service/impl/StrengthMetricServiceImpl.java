package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.dto.StrengthMetricBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.StrengthMetricCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.StrengthMetricUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.StrengthMetricEntity;
import com.company.officialwebsite.modules.site.mapper.StrengthMetricMapper;
import com.company.officialwebsite.modules.site.service.StrengthMetricService;
import com.company.officialwebsite.modules.site.vo.AdminStrengthMetricVO;
import com.company.officialwebsite.modules.site.vo.PortalStrengthMetricVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
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
 * StrengthMetricServiceImpl：实现企业实力核心指标大盘的后台维护、审计和前台缓存逻辑。
 *
 * <p>关键约束：
 * <ul>
 *   <li>写操作全部在事务内完成，缓存失效通过 PortalCacheInvalidationSupport 在事务提交后触发。</li>
 *   <li>iconId 允许为 null，媒体绑定/解绑逻辑根据 old/new iconId 变化情况精确处理。</li>
 *   <li>label 唯一性由数据库 uk_cms_strength_metric_label_deleted 保障，DuplicateKeyException 映射为业务异常。</li>
 *   <li>批量排序使用有序 ID 列表模式，后端计算 sort_order = (index+1) * GAP，防止并发排序值冲突。</li>
 * </ul>
 */
@Service
public class StrengthMetricServiceImpl implements StrengthMetricService {

    private static final Logger log = LoggerFactory.getLogger(StrengthMetricServiceImpl.class);

    /** Portal 缓存段名，拼接后完整 key 为 official:portal:strength_metrics。 */
    private static final String CACHE_SEGMENT = "strength_metrics";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "STRENGTH_METRIC";
    private static final String ACTION_CREATE = "CREATE_STRENGTH_METRIC";
    private static final String ACTION_UPDATE = "UPDATE_STRENGTH_METRIC";
    private static final String ACTION_DELETE = "DELETE_STRENGTH_METRIC";
    private static final String ACTION_REORDER = "REORDER_STRENGTH_METRIC";
    private static final String MEDIA_BIZ_FIELD = "icon";

    private final int sortGap;

    private final StrengthMetricMapper strengthMetricMapper;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheInvalidationSupport portalCacheInvalidationSupport;
    private final PortalCacheKeyBuilder portalCacheKeyBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OfficialProperties officialProperties;
    private final ObjectMapper objectMapper;

    public StrengthMetricServiceImpl(
            StrengthMetricMapper strengthMetricMapper,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            PortalCacheInvalidationSupport portalCacheInvalidationSupport,
            PortalCacheKeyBuilder portalCacheKeyBuilder,
            RedisTemplate<String, Object> redisTemplate,
            OfficialProperties officialProperties,
            ObjectMapper objectMapper) {
        this.strengthMetricMapper = strengthMetricMapper;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheInvalidationSupport = portalCacheInvalidationSupport;
        this.portalCacheKeyBuilder = portalCacheKeyBuilder;
        this.redisTemplate = redisTemplate;
        this.officialProperties = officialProperties;
        this.objectMapper = objectMapper;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    // ─────────────────────────────────────── Admin 写操作 ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<AdminStrengthMetricVO> getAdminMetrics() {
        return listActiveMetrics().stream().map(this::toAdminVO).toList();
    }

    @Override
    @Transactional
    public List<AdminStrengthMetricVO> createMetric(StrengthMetricCreateRequestDTO requestDTO) {
        StrengthMetricEntity entity = new StrengthMetricEntity();
        applyForCreate(entity, requestDTO);
        try {
            strengthMetricMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            // 数据库唯一约束 uk_cms_strength_metric_label_deleted 触发
            throw new BusinessException(ErrorCode.SITE_STRENGTH_METRIC_LABEL_DUPLICATE);
        }
        // 新建成功后绑定图标媒体资源（iconId 为 null 时 bindMedia 内部会跳过处理）
        handleIconBinding(null, entity.getIconId(), entity.getId());
        log.info("create strength metric success metricId={} label={} sortOrder={}",
                entity.getId(), entity.getLabel(), entity.getSortOrder());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalMetrics();
        return getAdminMetrics();
    }

    @Override
    @Transactional
    public List<AdminStrengthMetricVO> updateMetric(Long metricId, StrengthMetricUpdateRequestDTO requestDTO) {
        StrengthMetricEntity entity = requireActiveMetric(metricId);
        assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        Long oldIconId = entity.getIconId();
        applyForUpdate(entity, requestDTO);
        try {
            tryUpdate(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_STRENGTH_METRIC_LABEL_DUPLICATE);
        }
        // 精确处理图标绑定：只有 iconId 发生变化时才更新媒体引用
        if (!Objects.equals(oldIconId, entity.getIconId())) {
            handleIconBinding(oldIconId, entity.getIconId(), entity.getId());
        }
        log.info("update strength metric success metricId={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalMetrics();
        return getAdminMetrics();
    }

    @Override
    @Transactional
    public List<AdminStrengthMetricVO> deleteMetric(Long metricId, Integer version) {
        StrengthMetricEntity entity = requireActiveMetric(metricId);
        assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = strengthMetricMapper.delete(
                new LambdaUpdateWrapper<StrengthMetricEntity>()
                        .eq(StrengthMetricEntity::getId, entity.getId())
                        .eq(StrengthMetricEntity::getVersion, version));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "指标已被其他操作更新，请刷新后重试");
        }
        handleIconBinding(entity.getIconId(), null, entity.getId());
        log.info("delete strength metric success metricId={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalMetrics();
        return getAdminMetrics();
    }

    @Override
    @Transactional
    public List<AdminStrengthMetricVO> reorderMetrics(StrengthMetricBatchSortRequestDTO requestDTO) {
        List<StrengthMetricEntity> metrics = listActiveMetrics();
        if (metrics.isEmpty()) {
            throw new BusinessException(ErrorCode.SITE_STRENGTH_METRIC_NOT_FOUND, "暂无可排序的指标");
        }
        // 去重校验，防止前端传入重复 ID
        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedMetricIds());
        if (requestedOrder.size() != requestDTO.getOrderedMetricIds().size()) {
            log.warn("reorder duplicate ids detected original={} deduplicated={}",
                    requestDTO.getOrderedMetricIds(), requestedOrder);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复指标");
        }
        // 完整性校验：传入 ID 必须完整覆盖全部活跃指标，不允许遗漏
        Set<Long> currentIds = new LinkedHashSet<>(metrics.stream().map(StrengthMetricEntity::getId).toList());
        if (!new LinkedHashSet<>(requestedOrder).equals(currentIds)) {
            log.warn("reorder completeness check failed requested={} current={}", requestedOrder, currentIds);
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前全部活跃指标");
        }

        Map<Long, StrengthMetricEntity> entityMap = new HashMap<>();
        for (StrengthMetricEntity metric : metrics) {
            entityMap.put(metric.getId(), metric);
        }
        List<Map<String, Object>> before = metrics.stream()
                .sorted(metricComparator())
                .map(this::toSnapshot)
                .toList();

        // 按传入顺序依次写入新 sort_order，同一事务内保证原子性
        for (int index = 0; index < requestedOrder.size(); index++) {
            StrengthMetricEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            tryUpdate(entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();
        log.info("reorder strength metrics success metricCount={} order={}", requestedOrder.size(), requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalMetrics();
        return getAdminMetrics();
    }

    // ─────────────────────────────────────── Portal 只读 ───────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<PortalStrengthMetricVO> getPortalMetrics() {
        String cacheKey = portalCacheKeyBuilder.build(CACHE_SEGMENT);
        // 优先读缓存，读取失败时降级走数据库，不阻断请求
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.convertValue(
                        cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, PortalStrengthMetricVO.class));
            }
        } catch (Exception ex) {
            log.warn("read portal strength metrics cache failed key={}", cacheKey, ex);
        }

        List<PortalStrengthMetricVO> result = listVisibleMetrics().stream().map(this::toPortalVO).toList();
        // 写缓存失败不影响主流程
        try {
            Duration ttl = officialProperties.getCache().getDefaultTtl();
            redisTemplate.opsForValue().set(cacheKey, result, ttl);
        } catch (Exception ex) {
            log.warn("write portal strength metrics cache failed key={}", cacheKey, ex);
        }
        return result;
    }

    // ─────────────────────────────────────── 内部辅助方法 ───────────────────────────────────────

    /**
     * 新建时填充 entity，sortOrder 取当前最大值 + GAP 追加到末尾。
     */
    private void applyForCreate(StrengthMetricEntity entity, StrengthMetricCreateRequestDTO requestDTO) {
        entity.setMetricValue(normalizeMetricValue(requestDTO.getMetricValue()));
        entity.setLabel(normalizeLabel(requestDTO.getLabel()));
        entity.setIconId(resolveIconId(requestDTO.getIconId()));
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(nextSortOrder());
    }

    /**
     * 编辑时更新 entity 字段，不修改 sortOrder（排序由 batchSort 单独维护）。
     */
    private void applyForUpdate(StrengthMetricEntity entity, StrengthMetricUpdateRequestDTO requestDTO) {
        entity.setMetricValue(normalizeMetricValue(requestDTO.getMetricValue()));
        entity.setLabel(normalizeLabel(requestDTO.getLabel()));
        entity.setIconId(resolveIconId(requestDTO.getIconId()));
        entity.setVisible(requestDTO.getVisible());
    }

    /**
     * 查询单条活跃指标，不存在或已删除时抛出业务异常。
     */
    private StrengthMetricEntity requireActiveMetric(Long metricId) {
        StrengthMetricEntity entity = strengthMetricMapper.selectOne(
                new LambdaQueryWrapper<StrengthMetricEntity>()
                        .eq(StrengthMetricEntity::getId, metricId)
                        .eq(StrengthMetricEntity::getDeletedMarker, 0L));
        if (entity == null) {
            log.warn("strength metric not found metricId={}", metricId);
            throw new BusinessException(ErrorCode.SITE_STRENGTH_METRIC_NOT_FOUND);
        }
        return entity;
    }

    /**
     * 查询全部活跃指标，按 sort_order 升序、id 升序（保证排序稳定）。
     */
    private List<StrengthMetricEntity> listActiveMetrics() {
        return strengthMetricMapper.selectList(
                new LambdaQueryWrapper<StrengthMetricEntity>()
                        .eq(StrengthMetricEntity::getDeletedMarker, 0L)
                        .orderByAsc(StrengthMetricEntity::getSortOrder)
                        .orderByAsc(StrengthMetricEntity::getId));
    }

    /**
     * 查询前台可见指标（visible=1 且未删除），按 sort_order/id 升序。
     */
    private List<StrengthMetricEntity> listVisibleMetrics() {
        return strengthMetricMapper.selectList(
                new LambdaQueryWrapper<StrengthMetricEntity>()
                        .eq(StrengthMetricEntity::getDeletedMarker, 0L)
                        .eq(StrengthMetricEntity::getVisible, true)
                        .orderByAsc(StrengthMetricEntity::getSortOrder)
                        .orderByAsc(StrengthMetricEntity::getId));
    }

    /**
     * Admin VO 组装：icon 信息来自 mediaAssetService，iconId 为 null 时图标字段返回空字符串。
     */
    private String resolveIconUrl(Long iconId, Long metricId) {
        if (iconId == null) {
            return "";
        }
        try {
            MediaAssetEntity icon = mediaAssetService.requireUsableImage(iconId);
            return StringFieldUtils.defaultString(icon.getPublicUrl());
        } catch (BusinessException ex) {
            log.warn("strength metric icon unavailable metricId={} iconId={}", metricId, iconId);
            return "";
        }
    }

    private AdminStrengthMetricVO toAdminVO(StrengthMetricEntity entity) {
        AdminStrengthMetricVO vo = new AdminStrengthMetricVO();
        vo.setId(entity.getId());
        vo.setMetricValue(StringFieldUtils.defaultString(entity.getMetricValue()));
        vo.setLabel(StringFieldUtils.defaultString(entity.getLabel()));
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setIconId(entity.getIconId());
        vo.setIconUrl(resolveIconUrl(entity.getIconId(), entity.getId()));
        if (entity.getIconId() != null) {
            try {
                MediaAssetEntity icon = mediaAssetService.requireUsableImage(entity.getIconId());
                vo.setIconFileName(StringFieldUtils.defaultString(icon.getOriginalFilename()));
            } catch (BusinessException ex) {
                vo.setIconFileName("");
            }
        } else {
            vo.setIconFileName("");
        }
        return vo;
    }

    private PortalStrengthMetricVO toPortalVO(StrengthMetricEntity entity) {
        PortalStrengthMetricVO vo = new PortalStrengthMetricVO();
        vo.setId(entity.getId());
        vo.setMetricValue(StringFieldUtils.defaultString(entity.getMetricValue()));
        vo.setLabel(StringFieldUtils.defaultString(entity.getLabel()));
        vo.setIconUrl(resolveIconUrl(entity.getIconId(), entity.getId()));
        return vo;
    }

    /**
     * 生成操作快照，用于审计日志的 before/after 记录。
     */
    private Map<String, Object> toSnapshot(StrengthMetricEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("metricValue", entity.getMetricValue());
        snapshot.put("label", entity.getLabel());
        snapshot.put("iconId", entity.getIconId());
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    /**
     * 校验并解析 iconId：为 null 时直接返回 null，不为 null 时验证媒体资源可用性。
     * 图标校验失败映射为 SITE_STRENGTH_METRIC_ICON_INVALID。
     */
    private Long resolveIconId(Long iconId) {
        if (iconId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(iconId).getId();
        } catch (BusinessException ex) {
            log.warn("strength metric icon validation failed iconId={}", iconId);
            throw new BusinessException(ErrorCode.SITE_STRENGTH_METRIC_ICON_INVALID);
        }
    }

    /**
     * 根据 oldIconId 和 newIconId 精确处理媒体绑定。
     * 仅在 iconId 发生变化时调用，避免无谓的媒体表读写。
     *
     * <p>状态转换矩阵：
     * <ul>
     *   <li>null → null：不调用（调用方已保证）</li>
     *   <li>null → newId：bindMedia(newId) 绑定新图标</li>
     *   <li>oldId → null：bindMedia(null) 解绑旧图标</li>
     *   <li>oldId → newId：bindMedia(newId) 绑定新图标，旧图标引用由 mediaAssetService 内部处理</li>
     * </ul>
     */
    private void handleIconBinding(Long oldIconId, Long newIconId, Long bizObjectId) {
        // 任何方向的变化都调用 bindMedia，由 mediaAssetService 负责计算引用差异和状态同步
        mediaAssetService.bindMedia(newIconId, BIZ_MODULE, bizObjectId, MEDIA_BIZ_FIELD);
    }

    /**
     * 执行乐观锁更新，updateById 内部依赖 @Version 自动递增版本号。
     * 更新失败说明版本已被其他事务修改，抛出状态冲突异常。
     */
    private void tryUpdate(StrengthMetricEntity entity) {
        Integer requestVersion = entity.getVersion();
        int updated = strengthMetricMapper.updateById(entity);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "指标已被其他操作更新，请刷新后重试");
        }
        // MyBatis Plus @Version 会自动递增，手动同步本地对象版本以保证后续 tryUpdate 正确
        if (entity.getVersion() == null || entity.getVersion().equals(requestVersion)) {
            entity.setVersion(requestVersion + 1);
        }
    }

    /**
     * 校验请求版本号是否与数据库当前版本一致，防止并发覆盖。
     */
    private void assertVersion(Integer currentVersion, Integer requestVersion) {
        if (requestVersion == null || requestVersion < 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "版本号不能为负数");
        }
        if (!Objects.equals(currentVersion, requestVersion)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "指标已被其他操作更新，请刷新后重试");
        }
    }

    /**
     * 计算新增指标的 sortOrder：取当前最大 sortOrder + GAP，默认从 GAP 起步。
     */
    private int nextSortOrder() {
        StrengthMetricEntity last = strengthMetricMapper.selectOne(
                new LambdaQueryWrapper<StrengthMetricEntity>()
                        .eq(StrengthMetricEntity::getDeletedMarker, 0L)
                        .orderByDesc(StrengthMetricEntity::getSortOrder)
                        .orderByDesc(StrengthMetricEntity::getId)
                        .last("limit 1"));
        int current = (last == null || last.getSortOrder() == null) ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值已达到上限，请先整理现有指标");
        }
        return current + sortGap;
    }

    /**
     * 将 0-based 排序索引转换为 sort_order 值（1-based * GAP）。
     */
    private int sortOrderForIndex(int index) {
        try {
            return Math.multiplyExact(Math.addExact(index, 1), sortGap);
        } catch (ArithmeticException ex) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值超出允许范围");
        }
    }

    /**
     * 对传入的 ID 列表去重，同时校验每个 ID 不为 null。
     */
    private List<Long> deduplicateIds(Collection<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序指标 ID 不能为空");
            }
            deduplicated.add(id);
        }
        return new ArrayList<>(deduplicated);
    }

    private String normalizeMetricValue(String value) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "核心数值不能为空");
        }
        return normalized;
    }

    private String normalizeLabel(String label) {
        String normalized = StringFieldUtils.trimToNull(label);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "业务标签不能为空");
        }
        return normalized;
    }

    private Comparator<StrengthMetricEntity> metricComparator() {
        return Comparator
                .comparing(StrengthMetricEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(StrengthMetricEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    /**
     * 触发 Portal 缓存失效，必须在事务内调用。
     * PortalCacheInvalidationSupport 内部通过 afterCommit 保证提交后才删除缓存。
     */
    private void invalidatePortalMetrics() {
        portalCacheInvalidationSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
