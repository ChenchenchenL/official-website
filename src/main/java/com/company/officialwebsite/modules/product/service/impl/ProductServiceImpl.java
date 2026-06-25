package com.company.officialwebsite.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.product.converter.ProductConverter;
import com.company.officialwebsite.modules.product.dto.ProductCreateDTO;
import com.company.officialwebsite.modules.product.dto.ProductSortItemDTO;
import com.company.officialwebsite.modules.product.dto.ProductUpdateDTO;
import com.company.officialwebsite.modules.product.entity.ProductEntity;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import com.company.officialwebsite.modules.product.service.ProductService;
import com.company.officialwebsite.modules.product.vo.PortalProductVO;
import com.company.officialwebsite.modules.product.vo.ProductVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
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
 * ProductServiceImpl：实现产品矩阵与产品信息管理服务的具体逻辑，包括并发控制、Logo文件生命周期校验及绑定、延迟双删及操作审计。
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private static final String BIZ_MODULE = "PRODUCT";
    private static final String TARGET_TYPE = "PRODUCT";
    private static final String ACTION_CREATE = "CREATE_PRODUCT";
    private static final String ACTION_UPDATE = "UPDATE_PRODUCT";
    private static final String ACTION_DELETE = "DELETE_PRODUCT";
    private static final String ACTION_REORDER = "REORDER_PRODUCT";
    private static final String MEDIA_BIZ_FIELD = "logo";
    private static final String CACHE_KEY_SEGMENT = "products";

    private final ProductMapper productMapper;
    private final ProductConverter productConverter;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheInvalidationSupport portalCacheInvalidationSupport;
    private final PortalCacheKeyBuilder portalCacheKeyBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OfficialProperties officialProperties;
    private final ObjectMapper objectMapper;
    private final int sortGap;

    public ProductServiceImpl(
            ProductMapper productMapper,
            ProductConverter productConverter,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            PortalCacheInvalidationSupport portalCacheInvalidationSupport,
            PortalCacheKeyBuilder portalCacheKeyBuilder,
            RedisTemplate<String, Object> redisTemplate,
            OfficialProperties officialProperties,
            ObjectMapper objectMapper) {
        this.productMapper = productMapper;
        this.productConverter = productConverter;
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
    public PageResult<ProductVO> getProductList(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize <= 0 ? 20 : Math.min(pageSize, 100);
        Page<ProductEntity> page = productMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<ProductEntity>()
                        .eq(ProductEntity::getDeletedMarker, 0L)
                        .orderByAsc(ProductEntity::getSortOrder)
                        .orderByAsc(ProductEntity::getId));
        List<ProductVO> list = page.getRecords().stream().map(productConverter::toAdminVO).toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public Long createProduct(ProductCreateDTO createDTO) {
        validateDetailLink(createDTO.getDetailLink());

        ProductEntity entity = new ProductEntity();
        entity.setName(createDTO.getName().trim());
        entity.setSubTitle(createDTO.getSubTitle() != null ? createDTO.getSubTitle().trim() : null);
        entity.setAbstractText(createDTO.getAbstractText().trim());
        entity.setStatusTag(createDTO.getStatusTag() != null ? createDTO.getStatusTag().trim() : null);
        entity.setDetailLink(createDTO.getDetailLink() != null ? createDTO.getDetailLink().trim() : null);
        entity.setVisible(createDTO.getVisible() != null ? createDTO.getVisible() : 1);

        if (createDTO.getLogoId() != null) {
            try {
                mediaAssetService.requireUsableImage(createDTO.getLogoId());
            } catch (BusinessException ex) {
                log.warn("product logo validation failed logoId={}", createDTO.getLogoId());
                throw new BusinessException(ErrorCode.PRODUCT_LOGO_INVALID);
            }
            entity.setLogoId(createDTO.getLogoId());
        }

        if (createDTO.getSortOrder() != null) {
            entity.setSortOrder(createDTO.getSortOrder());
        } else {
            entity.setSortOrder(nextSortOrder());
        }

        try {
            productMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("create product duplicate name name={}", entity.getName());
            throw new BusinessException(ErrorCode.PRODUCT_NAME_DUPLICATE);
        }

        if (entity.getLogoId() != null) {
            mediaAssetService.bindMedia(entity.getLogoId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        }

        log.info("create product success productId={} name={} sortOrder={}",
                entity.getId(), entity.getName(), entity.getSortOrder());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalCache();
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateProduct(Long id, ProductUpdateDTO updateDTO) {
        validateDetailLink(updateDTO.getDetailLink());

        ProductEntity entity = requireActiveProduct(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), updateDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        Long oldLogoId = entity.getLogoId();

        entity.setName(updateDTO.getName().trim());
        entity.setSubTitle(updateDTO.getSubTitle() != null ? updateDTO.getSubTitle().trim() : null);
        entity.setAbstractText(updateDTO.getAbstractText().trim());
        entity.setStatusTag(updateDTO.getStatusTag() != null ? updateDTO.getStatusTag().trim() : null);
        entity.setDetailLink(updateDTO.getDetailLink() != null ? updateDTO.getDetailLink().trim() : null);
        entity.setVisible(updateDTO.getVisible() != null ? updateDTO.getVisible() : 1);
        if (updateDTO.getSortOrder() != null) {
            entity.setSortOrder(updateDTO.getSortOrder());
        }

        try {
            mediaAssetService.requireUsableImage(updateDTO.getLogoId());
        } catch (BusinessException ex) {
            log.warn("product logo validation failed logoId={}", updateDTO.getLogoId());
            throw new BusinessException(ErrorCode.PRODUCT_LOGO_INVALID);
        }
        entity.setLogoId(updateDTO.getLogoId());

        try {
            ConcurrencyHelper.tryUpdate(productMapper, entity);
        } catch (DuplicateKeyException ex) {
            log.warn("update product duplicate name productId={} name={}", entity.getId(), entity.getName());
            throw new BusinessException(ErrorCode.PRODUCT_NAME_DUPLICATE);
        }

        if (!Objects.equals(oldLogoId, entity.getLogoId())) {
            mediaAssetService.bindMedia(entity.getLogoId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        }

        log.info("update product success productId={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, Integer version) {
        ProductEntity entity = requireActiveProduct(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);

        int deleted = productMapper.delete(
                new LambdaUpdateWrapper<ProductEntity>()
                        .eq(ProductEntity::getId, entity.getId())
                        .eq(ProductEntity::getVersion, version));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }

        // 解绑 Logo 媒体资源
        mediaAssetService.bindMedia(null, BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);

        log.info("delete product success productId={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalCache();
    }

    @Override
    @Transactional
    public void batchSort(List<ProductSortItemDTO> sortItems) {
        if (sortItems == null || sortItems.isEmpty()) {
            log.warn("batch sort received empty list");
            return;
        }

        List<Long> requestedIds = sortItems.stream().map(ProductSortItemDTO::getId).toList();
        Set<Long> deduplicatedIds = new LinkedHashSet<>(requestedIds);
        if (deduplicatedIds.size() != requestedIds.size()) {
            log.warn("batch sort duplicate ids detected requestedIds={}", requestedIds);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复产品");
        }

        List<ProductEntity> activeProducts = productMapper.selectList(
                new LambdaQueryWrapper<ProductEntity>()
                        .eq(ProductEntity::getDeletedMarker, 0L)
                        .orderByAsc(ProductEntity::getId));
        if (activeProducts.isEmpty()) {
            log.warn("no active products to sort");
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "暂无可排序的产品");
        }

        Set<Long> currentIds = new LinkedHashSet<>(activeProducts.stream().map(ProductEntity::getId).toList());
        if (!deduplicatedIds.equals(currentIds)) {
            log.warn("batch sort completeness check failed requestedIds={} currentIds={}", deduplicatedIds, currentIds);
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前全部活跃产品");
        }

        Map<Long, ProductEntity> entityMap = new HashMap<>();
        for (ProductEntity product : activeProducts) {
            entityMap.put(product.getId(), product);
        }

        List<Map<String, Object>> before = activeProducts.stream()
                .sorted(Comparator.comparing(ProductEntity::getSortOrder).thenComparing(ProductEntity::getId))
                .map(this::toSnapshot)
                .toList();

        for (ProductSortItemDTO item : sortItems) {
            ProductEntity entity = entityMap.get(item.getId());
            if (entity == null) {
                log.error("batch sort entity not found in map productId={}", item.getId());
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "产品已被删除，请刷新后重试");
            }
            entity.setSortOrder(item.getSortOrder());
            ConcurrencyHelper.tryUpdate(productMapper, entity);
        }

        List<Map<String, Object>> after = sortItems.stream()
                .map(item -> entityMap.get(item.getId()))
                .map(this::toSnapshot)
                .toList();

        log.info("batch sort products success count={}", sortItems.size());
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalCache();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalProductVO> getPortalProducts() {
        String cacheKey = portalCacheKeyBuilder.build(CACHE_KEY_SEGMENT);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.convertValue(
                        cached,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, PortalProductVO.class));
            }
        } catch (Exception ex) {
            log.warn("read portal products cache failed key={}", cacheKey, ex);
        }

        List<ProductEntity> list = productMapper.selectList(
                new LambdaQueryWrapper<ProductEntity>()
                        .eq(ProductEntity::getDeletedMarker, 0L)
                        .eq(ProductEntity::getVisible, 1)
                        .orderByAsc(ProductEntity::getSortOrder)
                        .orderByAsc(ProductEntity::getId));
        List<PortalProductVO> result = list.stream().map(productConverter::toPortalVO).toList();

        try {
            Duration ttl = officialProperties.getCache().getDefaultTtl();
            redisTemplate.opsForValue().set(cacheKey, result, ttl);
        } catch (Exception ex) {
            log.warn("write portal products cache failed key={}", cacheKey, ex);
        }
        return result;
    }

    private ProductEntity requireActiveProduct(Long id) {
        ProductEntity entity = productMapper.selectOne(
                new LambdaQueryWrapper<ProductEntity>()
                        .eq(ProductEntity::getId, id)
                        .eq(ProductEntity::getDeletedMarker, 0L));
        if (entity == null) {
            log.warn("product not found productId={}", id);
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return entity;
    }

    private void validateDetailLink(String detailLink) {
        if (detailLink == null || detailLink.isBlank()) {
            return;
        }
        String link = detailLink.trim();
        if (link.startsWith("http://") || link.startsWith("https://")) {
            try {
                java.net.URI uri = new java.net.URI(link);
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if (scheme == null || host == null || host.isBlank()) {
                    throw new IllegalArgumentException();
                }
            } catch (Exception ex) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "详情跳转链接外部链接格式不合法");
            }
        } else {
            if (!link.startsWith("/") || link.startsWith("//") || link.contains("#") || link.contains("?") || link.contains("://")) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "详情跳转链接内部路由格式不合法");
            }
        }
    }

    private int nextSortOrder() {
        ProductEntity last = productMapper.selectOne(
                new LambdaQueryWrapper<ProductEntity>()
                        .eq(ProductEntity::getDeletedMarker, 0L)
                        .orderByDesc(ProductEntity::getSortOrder)
                        .orderByDesc(ProductEntity::getId)
                        .last("limit 1"));
        int current = (last == null || last.getSortOrder() == null) ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值已达到上限，请先整理现有产品");
        }
        return current + sortGap;
    }

    private Map<String, Object> toSnapshot(ProductEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("name", entity.getName());
        snapshot.put("logoId", entity.getLogoId());
        snapshot.put("subTitle", entity.getSubTitle());
        snapshot.put("abstractText", entity.getAbstractText());
        snapshot.put("statusTag", entity.getStatusTag());
        snapshot.put("detailLink", entity.getDetailLink());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void invalidatePortalCache() {
        portalCacheInvalidationSupport.invalidatePortalKey(CACHE_KEY_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
