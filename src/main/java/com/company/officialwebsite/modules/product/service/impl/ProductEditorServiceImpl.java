package com.company.officialwebsite.modules.product.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.dto.DetailDraftSaveDTO;
import com.company.officialwebsite.common.dto.DetailOfflineDTO;
import com.company.officialwebsite.common.dto.DetailPublishDTO;
import com.company.officialwebsite.common.dto.DetailRollbackDTO;
import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.content.dto.DetailPreviewTokenData;
import com.company.officialwebsite.modules.content.service.ContentReferenceGuard;
import com.company.officialwebsite.modules.content.service.DetailPreviewTokenService;
import com.company.officialwebsite.modules.content.service.DetailValidationSupport;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.pagebuilder.service.PageCacheInvalidationService;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.company.officialwebsite.modules.product.entity.ProductDraftEntity;
import com.company.officialwebsite.modules.product.entity.ProductEntity;
import com.company.officialwebsite.modules.product.entity.ProductVersionEntity;
import com.company.officialwebsite.modules.product.mapper.ProductDraftMapper;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import com.company.officialwebsite.modules.product.mapper.ProductVersionMapper;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionMapper;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionEntity;
import com.company.officialwebsite.modules.product.service.ProductEditorService;
import com.company.officialwebsite.modules.product.vo.ProductDraftVO;
import com.company.officialwebsite.modules.product.vo.ProductVersionVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ProductEditorServiceImpl：产品详情编辑全生命周期业务实现类。
 */
@Service
public class ProductEditorServiceImpl implements ProductEditorService {

    private static final Logger log = LoggerFactory.getLogger(ProductEditorServiceImpl.class);

    private static final String BIZ_MODULE = "PRODUCT";
    private static final String TARGET_TYPE = "PRODUCT_DETAIL";
    private static final String ACTION_SAVE_DRAFT = "SAVE_PRODUCT_DRAFT";
    private static final String ACTION_PUBLISH = "PUBLISH_PRODUCT";
    private static final String ACTION_ROLLBACK = "ROLLBACK_PRODUCT";
    private static final String ACTION_OFFLINE = "OFFLINE_PRODUCT";

    private final ProductMapper productMapper;
    private final ProductDraftMapper draftMapper;
    private final ProductVersionMapper versionMapper;
    private final EditorLockService editorLockService;
    private final DetailPreviewTokenService detailPreviewTokenService;
    private final DetailValidationSupport detailValidationSupport;
    private final ContentReferenceGuard contentReferenceGuard;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final PageCacheInvalidationService pageCacheInvalidationService;
    private final ObjectMapper objectMapper;
    private final CaseMapper caseMapper;
    private final IndustrySolutionMapper industrySolutionMapper;

    public ProductEditorServiceImpl(
            ProductMapper productMapper,
            ProductDraftMapper draftMapper,
            ProductVersionMapper versionMapper,
            EditorLockService editorLockService,
            DetailPreviewTokenService detailPreviewTokenService,
            DetailValidationSupport detailValidationSupport,
            ContentReferenceGuard contentReferenceGuard,
            AuditLogService auditLogService,
            PortalCacheSupport portalCacheSupport,
            PageCacheInvalidationService pageCacheInvalidationService,
            ObjectMapper objectMapper,
            CaseMapper caseMapper,
            IndustrySolutionMapper industrySolutionMapper) {
        this.productMapper = productMapper;
        this.draftMapper = draftMapper;
        this.versionMapper = versionMapper;
        this.editorLockService = editorLockService;
        this.detailPreviewTokenService = detailPreviewTokenService;
        this.detailValidationSupport = detailValidationSupport;
        this.contentReferenceGuard = contentReferenceGuard;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.pageCacheInvalidationService = pageCacheInvalidationService;
        this.objectMapper = objectMapper;
        this.caseMapper = caseMapper;
        this.industrySolutionMapper = industrySolutionMapper;
    }

    @Override
    @Transactional
    public ProductDraftVO createDraftShell(String operator) {
        ProductEntity product = new ProductEntity();
        product.setName("未命名产品_" + System.currentTimeMillis());
        product.setLogoId(0L);
        product.setAbstractText("暂无摘要");
        product.setStatus("DRAFT");
        product.setVisible(0);
        product.setSortOrder(100);
        productMapper.insert(product);

        ProductDraftEntity draft = new ProductDraftEntity();
        draft.setProductId(product.getId());
        draft.setDraftJson("{\"name\":\"未命名产品\"}");
        draft.setDraftHash(computeHash(draft.getDraftJson()));
        draft.setCreatedBy(operator);
        draft.setUpdatedBy(operator);
        draftMapper.insert(draft);

        log.info("createDraftShell success productId={} draftId={}", product.getId(), draft.getId());
        return toDraftVO(draft);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDraftVO getDraft(Long productId) {
        requireProductExists(productId);
        ProductDraftEntity draft = queryDraftByProductId(productId);
        if (draft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "该产品的详情草稿不存在");
        }
        return toDraftVO(draft);
    }

    @Override
    @Transactional
    public ProductDraftVO saveDraft(Long productId, DetailDraftSaveDTO saveDTO, String lockToken, String operator) {
        // 门禁：校验独占锁
        editorLockService.validateLock(EditorResourceTypeEnum.PRODUCT, productId, lockToken, operator);

        ProductEntity product = requireProductExists(productId);
        ProductDraftEntity draft = queryDraftByProductId(productId);
        if (draft == null) {
            ConcurrencyHelper.assertVersion(0, saveDTO.getVersion());
            draft = new ProductDraftEntity();
            draft.setProductId(productId);
            draft.setVersion(0);
        } else {
            ConcurrencyHelper.assertVersion(draft.getVersion(), saveDTO.getVersion());
        }

        String jsonStr;
        try {
            JsonNode draftNode = objectMapper.valueToTree(saveDTO.getDraft());
            validateAndSanitizeDraft(draftNode, null);
            jsonStr = objectMapper.writeValueAsString(draftNode);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "草稿 JSON 格式无效", e);
        }

        String hash = computeHash(jsonStr);

        ProductDraftVO beforeSnapshot = toDraftVO(draft);

        draft.setDraftJson(jsonStr);
        draft.setDraftHash(hash);
        draft.setEditorSessionRemark(saveDTO.getEditorSessionRemark());
        draft.setUpdatedBy(operator);

        if (draft.getId() == null) {
            draftMapper.insert(draft);
        } else {
            int rows = draftMapper.updateById(draft);
            if (rows != 1) {
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
            }
        }

        ProductDraftVO afterSnapshot = toDraftVO(draft);
        auditLogService.recordGenericOperation(
                BIZ_MODULE, ACTION_SAVE_DRAFT, TARGET_TYPE, draft.getId(), beforeSnapshot, afterSnapshot);

        log.info("saveDraft product success productId={} draftId={} hash={}", productId, draft.getId(), hash);
        return getDraft(productId);
    }

    @Override
    @Transactional(readOnly = true)
    public String createPreviewToken(Long productId, String draftHash, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.PRODUCT, productId, lockToken, operator);
        ProductDraftEntity draft = queryDraftByProductId(productId);
        if (draft == null || draft.getDraftJson() == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "草稿内容为空，无法生成预览");
        }
        if (!draft.getDraftHash().equals(draftHash.trim())) {
            throw new BusinessException(ErrorCode.PAGE_PREVIEW_SCHEMA_HASH_MISMATCH, "草稿内容已变更，请先保存后再生成预览");
        }

        return detailPreviewTokenService.createToken(
                EditorResourceTypeEnum.PRODUCT, productId, draft.getDraftHash(), operator);
    }

    @Override
    @Transactional(readOnly = true)
    public Object renderPreview(Long productId, String previewToken, String operator) {
        DetailPreviewTokenData tokenData = detailPreviewTokenService.resolveToken(previewToken);
        if (!EditorResourceTypeEnum.PRODUCT.equals(tokenData.getResourceType()) || !productId.equals(tokenData.getResourceId())) {
            throw new BusinessException(ErrorCode.DETAIL_PREVIEW_TOKEN_EXPIRED, "预览 Token 与当前产品不匹配");
        }
        if (tokenData.getCreatedBy() != null && !tokenData.getCreatedBy().equalsIgnoreCase(operator)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "仅限创建该预览链接的管理员访问");
        }

        ProductDraftEntity draft = queryDraftByProductId(productId);
        if (draft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "该产品的详情草稿不存在");
        }
        if (!draft.getDraftHash().equals(tokenData.getDraftHash())) {
            throw new BusinessException(ErrorCode.DETAIL_PREVIEW_TOKEN_EXPIRED, "预览链接已失效，草稿已更新，请重新生成预览");
        }
        try {
            return objectMapper.readValue(draft.getDraftJson(), Object.class);
        } catch (Exception e) {
            return draft.getDraftJson();
        }
    }

    @Override
    @Transactional
    public void revokePreviewToken(Long productId, String previewToken, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.PRODUCT, productId, lockToken, operator);
        detailPreviewTokenService.revokeToken(previewToken);
    }

    @Override
    @Transactional
    public ProductVersionVO publish(Long productId, DetailPublishDTO publishDTO, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.PRODUCT, productId, lockToken, operator);

        ProductEntity product = requireProductExists(productId);
        ProductDraftEntity draft = queryDraftByProductId(productId);
        if (draft == null || draft.getDraftJson() == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "草稿不存在或内容为空，无法发布");
        }
        ConcurrencyHelper.assertVersion(draft.getVersion(), publishDTO.getVersion());
        validateDraftJson(draft.getDraftJson());

        int nextVerNo = getNextVersionNo(productId);

        ProductVersionEntity version = new ProductVersionEntity();
        version.setProductId(productId);
        version.setVersionNo(nextVerNo);
        version.setSnapshotJson(draft.getDraftJson());
        version.setSnapshotHash(draft.getDraftHash());
        version.setChangeSummary(publishDTO.getChangeSummary().trim());
        version.setPublisher(operator);
        version.setPublishedAt(LocalDateTime.now());
        versionMapper.insert(version);

        // 更新实体状态为 PUBLISHED
        product.setStatus("PUBLISHED");
        product.setVisible(1);
        productMapper.updateById(product);

        // 审计
        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("productId", productId);
        logMap.put("versionNo", nextVerNo);
        logMap.put("publisher", operator);
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_PUBLISH, TARGET_TYPE, version.getId(), null, logMap);

        // 事务提交后缓存双失效
        invalidateProductCache(productId);

        log.info("publish product success productId={} versionNo={}", productId, nextVerNo);
        return toVersionVO(version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductVersionVO> listVersions(Long productId) {
        requireProductExists(productId);
        List<ProductVersionEntity> list = versionMapper.selectList(
                new LambdaQueryWrapper<ProductVersionEntity>()
                        .eq(ProductVersionEntity::getProductId, productId)
                        .orderByDesc(ProductVersionEntity::getVersionNo)
        );
        return list.stream().map(this::toVersionVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ProductVersionVO rollback(Long productId, Long targetVersionId, DetailRollbackDTO rollbackDTO, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.PRODUCT, productId, lockToken, operator);

        ProductEntity product = requireProductExists(productId);
        ProductVersionEntity targetVersion = versionMapper.selectById(targetVersionId);
        if (targetVersion == null || !targetVersion.getProductId().equals(productId)) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "指定的历史发布版本不存在");
        }
        ProductDraftEntity draft = queryDraftByProductId(productId);
        if (draft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "产品草稿不存在，无法回滚");
        }
        ConcurrencyHelper.assertVersion(draft.getVersion(), rollbackDTO.getVersion());
        contentReferenceGuard.assertNotReferencedByPage("product", "Product", productId);

        int nextVerNo = getNextVersionNo(productId);

        // 1. 产生全新的回滚版本实体，标注 rollbackSourceVersionId
        ProductVersionEntity rollbackVersion = new ProductVersionEntity();
        rollbackVersion.setProductId(productId);
        rollbackVersion.setVersionNo(nextVerNo);
        rollbackVersion.setSnapshotJson(targetVersion.getSnapshotJson());
        rollbackVersion.setSnapshotHash(targetVersion.getSnapshotHash());
        rollbackVersion.setChangeSummary("回滚至版本 No." + targetVersion.getVersionNo() + (rollbackDTO.getChangeSummary() != null ? " (" + rollbackDTO.getChangeSummary() + ")" : ""));
        rollbackVersion.setPublisher(operator);
        rollbackVersion.setRollbackSourceVersionId(targetVersion.getId());
        rollbackVersion.setPublishedAt(LocalDateTime.now());
        versionMapper.insert(rollbackVersion);

        // 2. 同步重置草稿
        draft.setDraftJson(targetVersion.getSnapshotJson());
        draft.setDraftHash(targetVersion.getSnapshotHash());
        draft.setEditorSessionRemark("版本回滚自动覆盖草稿");
        draft.setUpdatedBy(operator);
        draftMapper.updateById(draft);

        // 3. 更新产品状态
        product.setStatus("PUBLISHED");
        product.setVisible(1);
        productMapper.updateById(product);

        // 审计
        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("productId", productId);
        logMap.put("targetVersionId", targetVersionId);
        logMap.put("newVersionNo", nextVerNo);
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_ROLLBACK, TARGET_TYPE, rollbackVersion.getId(), null, logMap);

        // 缓存失效
        invalidateProductCache(productId);

        log.info("rollback product success productId={} targetVersionId={} newVersionNo={}", productId, targetVersionId, nextVerNo);
        return toVersionVO(rollbackVersion);
    }

    @Override
    @Transactional
    public void offline(Long productId, DetailOfflineDTO offlineDTO, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.PRODUCT, productId, lockToken, operator);

        ProductEntity product = requireProductExists(productId);
        ProductDraftEntity draft = queryDraftByProductId(productId);
        if (draft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "产品草稿不存在，无法下线");
        }
        ConcurrencyHelper.assertVersion(draft.getVersion(), offlineDTO.getVersion());

        // 校验已发布 ACTIVE 页面强引用拦截（若有页面依赖则抛 409 Conflict 阻止下线）
        contentReferenceGuard.assertNotReferencedByPage("product", "Product", productId);

        product.setStatus("OFFLINE");
        product.setVisible(0);
        productMapper.updateById(product);

        // 审计
        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("productId", productId);
        logMap.put("reason", offlineDTO.getReason());
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_OFFLINE, TARGET_TYPE, productId, null, logMap);

        // 缓存双失效
        invalidateProductCache(productId);

        log.info("offline product success productId={} reason={}", productId, offlineDTO.getReason());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProductEntity requireProductExists(Long productId) {
        ProductEntity entity = productMapper.selectById(productId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return entity;
    }

    private ProductDraftEntity queryDraftByProductId(Long productId) {
        return draftMapper.selectOne(
                new LambdaQueryWrapper<ProductDraftEntity>().eq(ProductDraftEntity::getProductId, productId)
        );
    }

    private int getNextVersionNo(Long productId) {
        List<ProductVersionEntity> list = versionMapper.selectList(
                new LambdaQueryWrapper<ProductVersionEntity>()
                        .eq(ProductVersionEntity::getProductId, productId)
                        .orderByDesc(ProductVersionEntity::getVersionNo)
        );
        if (list.isEmpty()) {
            return 1;
        }
        return list.get(0).getVersionNo() + 1;
    }

    private String computeHash(String json) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "SHA-256 算法不可用", e);
        }
    }

    private ProductDraftVO toDraftVO(ProductDraftEntity entity) {
        if (entity == null) {
            return null;
        }
        Object draftObj;
        try {
            draftObj = objectMapper.readValue(entity.getDraftJson(), Object.class);
        } catch (Exception e) {
            draftObj = entity.getDraftJson();
        }
        return new ProductDraftVO(
                entity.getId(), entity.getProductId(), draftObj, entity.getDraftHash(),
                entity.getEditorSessionRemark(), entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private ProductVersionVO toVersionVO(ProductVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        Object snapshotObj;
        try {
            snapshotObj = objectMapper.readValue(entity.getSnapshotJson(), Object.class);
        } catch (Exception e) {
            snapshotObj = entity.getSnapshotJson();
        }
        return new ProductVersionVO(
                entity.getId(), entity.getProductId(), entity.getVersionNo(), snapshotObj,
                entity.getSnapshotHash(), entity.getChangeSummary(), entity.getPublisher(),
                entity.getRollbackSourceVersionId(), entity.getPublishedAt(), entity.getCreatedAt()
        );
    }

    private void invalidateProductCache(Long productId) {
        portalCacheSupport.invalidate(portalCacheSupport.buildKey("products"));
        pageCacheInvalidationService.invalidateCacheByTarget("product", "Product", String.valueOf(productId));
    }

    /**
     * 对产品草稿通用 JSON 执行富文本净化、媒体、SEO、链接与关联 ID 校验。
     */
    private void validateAndSanitizeDraft(JsonNode node, String parentField) {
        if (node == null || node.isNull()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "草稿内容不能为空");
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            objectNode.fields().forEachRemaining(entry -> {
                String field = entry.getKey();
                JsonNode value = entry.getValue();
                String normalized = field.toLowerCase();
                if (value.isTextual()) {
                    if (normalized.contains("content") || normalized.contains("richtext") || normalized.contains("html") || normalized.equals("body")) {
                        objectNode.put(field, detailValidationSupport.cleanRichTextHtml(value.asText()));
                    }
                    if (normalized.endsWith("link") || normalized.endsWith("url")) {
                        detailValidationSupport.validateLinkProtocol(value.asText());
                    }
                }
                if (value.isNumber() && isMediaIdField(normalized)) {
                    detailValidationSupport.validateMediaUsable(value.asLong());
                }
                if (value.isNumber() && (normalized.endsWith("id") || normalized.endsWith("ids")) && value.asLong() <= 0) {
                    throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "关联资源 ID 必须为正数");
                }
                if (value.isNumber()) {
                    validateRelatedEntity(normalized, value.asLong());
                }
                validateAndSanitizeDraft(value, normalized);
            });
            JsonNode seo = objectNode.get("seo");
            if (seo != null && seo.isObject()) {
                detailValidationSupport.validateSeo(textValue(seo, "title"), textValue(seo, "description"));
            }
        } else if (node.isArray()) {
            for (JsonNode item : (ArrayNode) node) {
                if (item.isNumber()) {
                    if (item.asLong() <= 0) {
                        throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "关联资源 ID 必须为正数");
                    }
                    if (isMediaIdField(parentField)) {
                        detailValidationSupport.validateMediaUsable(item.asLong());
                    }
                    validateRelatedEntity(parentField, item.asLong());
                }
                validateAndSanitizeDraft(item, parentField);
            }
        }
    }

    private void validateDraftJson(String draftJson) {
        try {
            validateAndSanitizeDraft(objectMapper.readTree(draftJson), null);
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "草稿 JSON 格式无效", exception);
        }
    }

    private boolean isMediaIdField(String field) {
        return field != null && (field.equals("mediaid") || field.equals("mediaids")
                || field.equals("logoid") || field.equals("covermediaid")
                || field.equals("imageid") || field.equals("imageids")
                || field.equals("thumbnailid") || field.equals("thumbnailids"));
    }

    /**
     * 校验草稿中显式关联的案例和行业方案均存在且未逻辑删除。
     */
    private void validateRelatedEntity(String field, Long id) {
        if (field == null) {
            return;
        }
        if (field.equals("caseid") || field.equals("caseids") || field.equals("relatedcaseids")) {
            CaseEntity entity = caseMapper.selectById(id);
            if (entity == null) {
                throw new BusinessException(ErrorCode.CASE_NOT_FOUND, "关联案例不存在或已删除: " + id);
            }
        }
        if (field.equals("industrysolutionid") || field.equals("industrysolutionids")
                || field.equals("relatedindustrysolutionids")) {
            IndustrySolutionEntity entity = industrySolutionMapper.selectById(id);
            if (entity == null) {
                throw new BusinessException(ErrorCode.PRODUCT_SOLUTION_NOT_FOUND, "关联行业方案不存在或已删除: " + id);
            }
        }
    }

    private String textValue(JsonNode objectNode, String field) {
        JsonNode value = objectNode.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }
}
