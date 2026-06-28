package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardOrderRequestDTO;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardUpdateRequestDTO;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardVisibilityUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.HomeMetricCardEntity;
import com.company.officialwebsite.modules.site.mapper.HomeMetricCardMapper;
import com.company.officialwebsite.modules.site.service.HomeMetricCardService;
import com.company.officialwebsite.modules.site.vo.AdminHomeMetricCardVO;
import com.company.officialwebsite.modules.site.vo.PortalHomeMetricCardVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HomeMetricCardServiceImpl：实现首页核心数据指标卡片的后台维护、审计与前台缓存读取逻辑。
 */
@Service
public class HomeMetricCardServiceImpl implements HomeMetricCardService {

    private static final Logger log = LoggerFactory.getLogger(HomeMetricCardServiceImpl.class);
    private static final String CACHE_MODULE = "home";
    private static final String CACHE_SEGMENT = "metrics";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "HOME_METRIC_CARD";
    private static final String ACTION_CREATE = "CREATE_HOME_METRIC_CARD";
    private static final String ACTION_UPDATE = "UPDATE_HOME_METRIC_CARD";
    private static final String ACTION_CHANGE_VISIBILITY = "CHANGE_HOME_METRIC_CARD_VISIBILITY";
    private static final String ACTION_DELETE = "DELETE_HOME_METRIC_CARD";
    private static final String ACTION_REORDER = "REORDER_HOME_METRIC_CARD";
    private static final int MAX_METRIC_INTEGER_DIGITS = 12;
    private static final int MAX_METRIC_DECIMAL_DIGITS = 2;
    private static final Pattern METRIC_VALUE_PATTERN =
            Pattern.compile("^(0|[1-9]\\d{0," + (MAX_METRIC_INTEGER_DIGITS - 1) + "})(\\.\\d{1,"
                    + MAX_METRIC_DECIMAL_DIGITS + "})?$");

    private final HomeMetricCardMapper homeMetricCardMapper;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final int sortGap;

    public HomeMetricCardServiceImpl(
            HomeMetricCardMapper homeMetricCardMapper,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport) {
        this.homeMetricCardMapper = homeMetricCardMapper;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminHomeMetricCardVO> getAdminMetricCards() {
        return listActiveMetricCards().stream().map(this::toAdminVO).toList();
    }

    @Override
    @Transactional
    public List<AdminHomeMetricCardVO> createMetricCard(HomeMetricCardCreateRequestDTO requestDTO) {
        HomeMetricCardEntity entity = new HomeMetricCardEntity();
        applyForCreate(entity, requestDTO);
        homeMetricCardMapper.insert(entity);
        log.info("create home metric card success metricId={} visible={} sortOrder={}",
                entity.getId(), entity.getVisible(), entity.getSortOrder());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalMetrics();
        return getAdminMetricCards();
    }

    @Override
    @Transactional
    public List<AdminHomeMetricCardVO> updateMetricCard(Long metricId, HomeMetricCardUpdateRequestDTO requestDTO) {
        log.info("update home metric card request metricId={} version={}", metricId, requestDTO.getVersion());
        HomeMetricCardEntity entity = requireActiveMetricCard(metricId);
        assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        applyForUpdate(entity, requestDTO);
        tryUpdate(entity);
        log.info("update home metric card success metricId={} currentVersion={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalMetrics();
        return getAdminMetricCards();
    }

    @Override
    @Transactional
    public List<AdminHomeMetricCardVO> updateVisibility(
            Long metricId,
            HomeMetricCardVisibilityUpdateRequestDTO requestDTO) {
        log.info("update home metric card visibility request metricId={} version={} visible={}",
                metricId, requestDTO.getVersion(), requestDTO.getVisible());
        HomeMetricCardEntity entity = requireActiveMetricCard(metricId);
        assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        entity.setVisible(requestDTO.getVisible());
        tryUpdate(entity);
        log.info("change home metric card visibility success metricId={} visible={}",
                entity.getId(), entity.getVisible());
        recordAudit(ACTION_CHANGE_VISIBILITY, entity.getId(), before, toSnapshot(entity));
        invalidatePortalMetrics();
        return getAdminMetricCards();
    }

    @Override
    @Transactional
    public List<AdminHomeMetricCardVO> deleteMetricCard(Long metricId) {
        HomeMetricCardEntity entity = requireActiveMetricCard(metricId);
        Map<String, Object> before = toSnapshot(entity);
        logicallyDeleteById(entity);
        log.info("delete home metric card success metricId={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalMetrics();
        return getAdminMetricCards();
    }

    @Override
    @Transactional
    public List<AdminHomeMetricCardVO> reorderMetricCards(HomeMetricCardOrderRequestDTO requestDTO) {
        List<HomeMetricCardEntity> metricCards = listActiveMetricCards();
        if (metricCards.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "排序目标不存在");
        }

        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedMetricIds());
        if (requestedOrder.size() != requestDTO.getOrderedMetricIds().size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复卡片");
        }

        Set<Long> currentIds = new LinkedHashSet<>(metricCards.stream().map(HomeMetricCardEntity::getId).toList());
        if (!new LinkedHashSet<>(requestedOrder).equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前卡片");
        }

        Map<Long, HomeMetricCardEntity> entityMap = new HashMap<>();
        for (HomeMetricCardEntity metricCard : metricCards) {
            entityMap.put(metricCard.getId(), metricCard);
        }

        List<Map<String, Object>> before = metricCards.stream()
                .sorted(metricCardComparator())
                .map(this::toSnapshot)
                .toList();
        List<Long> beforeOrder = orderedMetricIds(metricCards.stream()
                .sorted(metricCardComparator())
                .toList());
        for (int index = 0; index < requestedOrder.size(); index++) {
            HomeMetricCardEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            tryUpdate(entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toSnapshot)
                .toList();
        log.info("reorder home metric cards success metricCount={} beforeOrder={} afterOrder={}",
                requestedOrder.size(), beforeOrder, requestedOrder);
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalMetrics();
        return getAdminMetricCards();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalHomeMetricCardVO> getPortalMetricCards() {
        String cacheKey = portalCacheSupport.buildKey(CACHE_MODULE, CACHE_SEGMENT);
        List<PortalHomeMetricCardVO> cached = portalCacheSupport.readListCache(cacheKey, PortalHomeMetricCardVO.class, CACHE_MODULE);
        if (cached != null) {
            return cached;
        }

        List<PortalHomeMetricCardVO> metricCards = listVisibleMetricCards().stream()
                .map(this::toPortalVO)
                .toList();
        portalCacheSupport.writeCache(cacheKey, metricCards, portalCacheSupport.isEmptyResult(metricCards), CACHE_MODULE);
        return metricCards;
    }

    private void applyForCreate(HomeMetricCardEntity entity, HomeMetricCardCreateRequestDTO requestDTO) {
        entity.setMetricValue(normalizeMetricValue(requestDTO.getValue()));
        entity.setMetricUnit(normalizeMetricUnit(requestDTO.getUnit()));
        entity.setDescription(normalizeDescription(requestDTO.getDescription()));
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(nextSortOrder());
    }

    private void applyForUpdate(HomeMetricCardEntity entity, HomeMetricCardUpdateRequestDTO requestDTO) {
        entity.setMetricValue(normalizeMetricValue(requestDTO.getValue()));
        entity.setMetricUnit(normalizeMetricUnit(requestDTO.getUnit()));
        entity.setDescription(normalizeDescription(requestDTO.getDescription()));
    }

    private HomeMetricCardEntity requireActiveMetricCard(Long metricId) {
        HomeMetricCardEntity entity = homeMetricCardMapper.selectOne(new LambdaQueryWrapper<HomeMetricCardEntity>()
                .eq(HomeMetricCardEntity::getId, metricId)
                .eq(HomeMetricCardEntity::getDeletedMarker, 0L)
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND);
        }
        return entity;
    }

    private List<HomeMetricCardEntity> listActiveMetricCards() {
        return homeMetricCardMapper.selectList(new LambdaQueryWrapper<HomeMetricCardEntity>()
                .eq(HomeMetricCardEntity::getDeletedMarker, 0L)
                .orderByAsc(HomeMetricCardEntity::getSortOrder)
                .orderByAsc(HomeMetricCardEntity::getId));
    }

    private List<HomeMetricCardEntity> listVisibleMetricCards() {
        return homeMetricCardMapper.selectList(new LambdaQueryWrapper<HomeMetricCardEntity>()
                .eq(HomeMetricCardEntity::getDeletedMarker, 0L)
                .eq(HomeMetricCardEntity::getVisible, true)
                .orderByAsc(HomeMetricCardEntity::getSortOrder)
                .orderByAsc(HomeMetricCardEntity::getId));
    }

    private AdminHomeMetricCardVO toAdminVO(HomeMetricCardEntity entity) {
        AdminHomeMetricCardVO vo = new AdminHomeMetricCardVO();
        vo.setId(entity.getId());
        vo.setValue(StringFieldUtils.defaultString(entity.getMetricValue()));
        vo.setUnit(StringFieldUtils.defaultString(entity.getMetricUnit()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private PortalHomeMetricCardVO toPortalVO(HomeMetricCardEntity entity) {
        PortalHomeMetricCardVO vo = new PortalHomeMetricCardVO();
        vo.setValue(StringFieldUtils.defaultString(entity.getMetricValue()));
        vo.setUnit(StringFieldUtils.defaultString(entity.getMetricUnit()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        return vo;
    }

    private Map<String, Object> toSnapshot(HomeMetricCardEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("value", entity.getMetricValue());
        snapshot.put("unit", StringFieldUtils.defaultString(entity.getMetricUnit()));
        snapshot.put("description", entity.getDescription());
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void tryUpdate(HomeMetricCardEntity entity) {
        Integer requestVersion = entity.getVersion();
        int updated = homeMetricCardMapper.updateById(entity);
        if (updated != 1) {
            log.warn("update home metric card failed due to optimistic lock metricId={} requestVersion={}",
                    entity.getId(), requestVersion);
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "卡片已被其他操作更新，请刷新后重试");
        }
        syncVersionAfterUpdate(entity, requestVersion);
    }

    private void logicallyDeleteById(HomeMetricCardEntity entity) {
        int deleted = homeMetricCardMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "卡片已被其他操作更新，请刷新后重试");
        }
    }

    private void invalidatePortalMetrics() {
        portalCacheSupport.invalidatePortalKey(CACHE_MODULE, CACHE_SEGMENT);
    }

    private void assertVersion(Integer currentVersion, Integer requestVersion) {
        if (requestVersion == null || requestVersion < 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "版本号不能为负数");
        }
        if (!Objects.equals(currentVersion, requestVersion)) {
            log.warn("home metric card stale version currentVersion={} requestVersion={}", currentVersion, requestVersion);
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "卡片已被其他操作更新，请刷新后重试");
        }
    }

    private String normalizeMetricValue(String value) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null || !METRIC_VALUE_PATTERN.matcher(normalized).matches()) {
            throw new BusinessException(
                    ErrorCode.SITE_HOME_METRIC_VALUE_INVALID,
                    "数值必须为非负数字，整数部分最多12位且最多支持2位小数");
        }
        return normalized;
    }

    /**
     * 单位为可选字段，未配置时持久化为 null，对外统一返回空字符串，避免 Portal 出现 null。
     */
    private String normalizeMetricUnit(String unit) {
        return StringFieldUtils.trimToNull(unit);
    }

    private String normalizeDescription(String description) {
        String normalized = StringFieldUtils.trimToNull(description);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "描述文案不能为空");
        }
        return normalized;
    }

    private List<Long> deduplicateIds(Collection<Long> orderedMetricIds) {
        if (orderedMetricIds == null) {
            return List.of();
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long orderedMetricId : orderedMetricIds) {
            if (orderedMetricId == null) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序卡片 ID 不能为空");
            }
            deduplicated.add(orderedMetricId);
        }
        return List.copyOf(deduplicated);
    }

    private int nextSortOrder() {
        HomeMetricCardEntity last = homeMetricCardMapper.selectOne(new LambdaQueryWrapper<HomeMetricCardEntity>()
                .eq(HomeMetricCardEntity::getDeletedMarker, 0L)
                .orderByDesc(HomeMetricCardEntity::getSortOrder)
                .orderByDesc(HomeMetricCardEntity::getId)
                .last("limit 1"));
        int current = last == null || last.getSortOrder() == null ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "卡片排序值已达到上限");
        }
        return current + sortGap;
    }

    private int sortOrderForIndex(int index) {
        try {
            return Math.multiplyExact(Math.addExact(index, 1), sortGap);
        } catch (ArithmeticException ex) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值超出允许范围");
        }
    }

    /**
     * MyBatis-Plus 乐观锁通常会把新版本号回写到实体；若未回写，则以请求版本号做一次兼容兜底。
     */
    private void syncVersionAfterUpdate(HomeMetricCardEntity entity, Integer requestVersion) {
        if (entity.getVersion() == null || entity.getVersion().equals(requestVersion)) {
            entity.setVersion(requestVersion + 1);
        }
    }

    private Comparator<HomeMetricCardEntity> metricCardComparator() {
        return Comparator
                .comparing(HomeMetricCardEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(HomeMetricCardEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private List<Long> orderedMetricIds(List<HomeMetricCardEntity> metricCards) {
        return metricCards.stream().map(HomeMetricCardEntity::getId).toList();
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
