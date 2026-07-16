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
import com.company.officialwebsite.modules.content.service.AbstractDetailEditorService;
import com.company.officialwebsite.modules.content.service.DetailPreviewTokenService;
import com.company.officialwebsite.modules.content.service.DetailValidationSupport;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.company.officialwebsite.modules.pagebuilder.service.PageCacheInvalidationService;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionDraftEntity;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionEntity;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionVersionEntity;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionDraftMapper;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionMapper;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionVersionMapper;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import com.company.officialwebsite.modules.product.service.IndustrySolutionEditorService;
import com.company.officialwebsite.modules.product.vo.IndustrySolutionDraftVO;
import com.company.officialwebsite.modules.product.vo.IndustrySolutionVersionVO;
import com.company.officialwebsite.modules.product.vo.PortalIndustrySolutionDetailVO;
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
 * IndustrySolutionEditorServiceImpl：行业解决方案详情编辑全生命周期业务实现类。
 */
@Service
public class IndustrySolutionEditorServiceImpl
        extends AbstractDetailEditorService<IndustrySolutionDraftVO, IndustrySolutionVersionVO>
        implements IndustrySolutionEditorService {

    private static final Logger log = LoggerFactory.getLogger(IndustrySolutionEditorServiceImpl.class);

    private static final String BIZ_MODULE = "INDUSTRY_SOLUTION";
    private static final String TARGET_TYPE = "INDUSTRY_SOLUTION_DETAIL";
    private static final String ACTION_SAVE_DRAFT = "SAVE_INDUSTRY_SOLUTION_DRAFT";
    private static final String ACTION_PUBLISH = "PUBLISH_INDUSTRY_SOLUTION";
    private static final String ACTION_ROLLBACK = "ROLLBACK_INDUSTRY_SOLUTION";
    private static final String ACTION_OFFLINE = "OFFLINE_INDUSTRY_SOLUTION";

    private final IndustrySolutionMapper solutionMapper;
    private final IndustrySolutionDraftMapper draftMapper;
    private final IndustrySolutionVersionMapper versionMapper;
    private final EditorLockService editorLockService;
    private final DetailPreviewTokenService detailPreviewTokenService;
    private final DetailValidationSupport detailValidationSupport;
    private final ContentReferenceGuard contentReferenceGuard;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final PageCacheInvalidationService pageCacheInvalidationService;
    private final ProductMapper productMapper;
    private final CaseMapper caseMapper;
    private final MediaAssetService mediaAssetService;
    private final ObjectMapper objectMapper;

    public IndustrySolutionEditorServiceImpl(
            IndustrySolutionMapper solutionMapper,
            IndustrySolutionDraftMapper draftMapper,
            IndustrySolutionVersionMapper versionMapper,
            EditorLockService editorLockService,
            DetailPreviewTokenService detailPreviewTokenService,
            DetailValidationSupport detailValidationSupport,
            ContentReferenceGuard contentReferenceGuard,
            AuditLogService auditLogService,
            PortalCacheSupport portalCacheSupport,
            PageCacheInvalidationService pageCacheInvalidationService,
            ProductMapper productMapper,
            CaseMapper caseMapper,
            MediaAssetService mediaAssetService,
            ObjectMapper objectMapper) {
        super(editorLockService);
        this.solutionMapper = solutionMapper;
        this.draftMapper = draftMapper;
        this.versionMapper = versionMapper;
        this.editorLockService = editorLockService;
        this.detailPreviewTokenService = detailPreviewTokenService;
        this.detailValidationSupport = detailValidationSupport;
        this.contentReferenceGuard = contentReferenceGuard;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.pageCacheInvalidationService = pageCacheInvalidationService;
        this.productMapper = productMapper;
        this.caseMapper = caseMapper;
        this.mediaAssetService = mediaAssetService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public IndustrySolutionDraftVO createDraftShell(String operator) {
        IndustrySolutionEntity solution = new IndustrySolutionEntity();
        solution.setName("未命名行业方案_" + System.currentTimeMillis());
        solution.setIconMediaId(0L);
        solution.setDescription("暂无描述");
        solution.setVisible(false);
        solution.setSortOrder(100);
        solutionMapper.insert(solution);

        IndustrySolutionDraftEntity draft = new IndustrySolutionDraftEntity();
        draft.setSolutionId(solution.getId());
        draft.setDraftJson("{\"name\":\"未命名行业方案\"}");
        draft.setDraftHash(computeHash(draft.getDraftJson()));
        draft.setCreatedBy(operator);
        draft.setUpdatedBy(operator);
        draftMapper.insert(draft);

        log.info("createDraftShell solution success solutionId={} draftId={}", solution.getId(), draft.getId());
        return toDraftVO(draft);
    }

    @Override
    @Transactional(readOnly = true)
    public IndustrySolutionDraftVO getDraft(Long solutionId) {
        requireSolutionExists(solutionId);
        IndustrySolutionDraftEntity draft = queryDraftBySolutionId(solutionId);
        if (draft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "该行业方案的详情草稿不存在");
        }
        return toDraftVO(draft);
    }

    @Override
    public EditorResourceTypeEnum getResourceType() {
        return EditorResourceTypeEnum.INDUSTRY_SOLUTION;
    }

    @Override
    @Transactional
    protected IndustrySolutionDraftVO doSaveDraft(Long solutionId, DetailDraftSaveDTO saveDTO, String operator) {

        requireSolutionExists(solutionId);
        IndustrySolutionDraftEntity draft = queryDraftBySolutionId(solutionId);
        if (draft == null) {
            draft = new IndustrySolutionDraftEntity();
            draft.setSolutionId(solutionId);
            draft.setVersion(0);
        } else {
            ConcurrencyHelper.assertVersion(draft.getVersion(), saveDTO.getVersion());
        }

        String jsonStr;
        try {
            JsonNode draftNode = objectMapper.valueToTree(saveDTO.getDraft());
            validateAndSanitizeDraft(draftNode, null);
            jsonStr = objectMapper.writeValueAsString(draftNode);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "草稿 JSON 格式无效", e);
        }

        String hash = computeHash(jsonStr);

        IndustrySolutionDraftVO beforeSnapshot = toDraftVO(draft);

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

        IndustrySolutionDraftVO afterSnapshot = toDraftVO(draft);
        auditLogService.recordGenericOperation(
                BIZ_MODULE, ACTION_SAVE_DRAFT, TARGET_TYPE, draft.getId(), beforeSnapshot, afterSnapshot);

        log.info("saveDraft solution success solutionId={} draftId={} hash={}", solutionId, draft.getId(), hash);
        return getDraft(solutionId);
    }

    @Override
    @Transactional(readOnly = true)
    public String createPreviewToken(Long solutionId, String draftHash, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.INDUSTRY_SOLUTION, solutionId, lockToken, operator);
        IndustrySolutionDraftEntity draft = queryDraftBySolutionId(solutionId);
        if (draft == null || draft.getDraftJson() == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "草稿内容为空，无法生成预览");
        }
        if (!draft.getDraftHash().equals(draftHash.trim())) {
            throw new BusinessException(ErrorCode.PAGE_PREVIEW_SCHEMA_HASH_MISMATCH, "草稿内容已变更，请先保存后再生成预览");
        }

        return detailPreviewTokenService.createToken(
                EditorResourceTypeEnum.INDUSTRY_SOLUTION, solutionId, draft.getDraftHash(), operator);
    }

    @Override
    @Transactional(readOnly = true)
    public Object renderPreview(Long solutionId, String previewToken, String operator) {
        DetailPreviewTokenData tokenData = detailPreviewTokenService.resolveToken(previewToken);
        if (!EditorResourceTypeEnum.INDUSTRY_SOLUTION.equals(tokenData.getResourceType()) || !solutionId.equals(tokenData.getResourceId())) {
            throw new BusinessException(ErrorCode.DETAIL_PREVIEW_TOKEN_EXPIRED, "预览 Token 与当前行业方案不匹配");
        }
        if (tokenData.getCreatedBy() != null && !tokenData.getCreatedBy().equalsIgnoreCase(operator)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "仅限创建该预览链接的管理员访问");
        }

        IndustrySolutionDraftEntity draft = queryDraftBySolutionId(solutionId);
        if (draft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "该行业方案的详情草稿不存在");
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
    public void revokePreviewToken(Long solutionId, String previewToken, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.INDUSTRY_SOLUTION, solutionId, lockToken, operator);
        detailPreviewTokenService.revokeToken(previewToken);
    }

    @Override
    @Transactional
    protected IndustrySolutionVersionVO doPublish(Long solutionId, DetailPublishDTO publishDTO, String operator) {

        IndustrySolutionEntity solution = requireSolutionExists(solutionId);
        IndustrySolutionDraftEntity draft = queryDraftBySolutionId(solutionId);
        if (draft == null || draft.getDraftJson() == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "草稿不存在或内容为空，无法发布");
        }
        ConcurrencyHelper.assertVersion(draft.getVersion(), publishDTO.getVersion());
        validateDraftJson(draft.getDraftJson());

        int nextVerNo = getNextVersionNo(solutionId);

        IndustrySolutionVersionEntity version = new IndustrySolutionVersionEntity();
        version.setSolutionId(solutionId);
        version.setVersionNo(nextVerNo);
        version.setSnapshotJson(draft.getDraftJson());
        version.setSnapshotHash(draft.getDraftHash());
        version.setChangeSummary(publishDTO.getChangeSummary().trim());
        version.setPublisher(operator);
        version.setPublishedAt(LocalDateTime.now());
        versionMapper.insert(version);
        mediaAssetService.bindPublishedSnapshotMedia(
                "INDUSTRY_SOLUTION_VERSION", version.getId(), version.getSnapshotJson());

        solution.setVisible(true);
        solutionMapper.updateById(solution);

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("solutionId", solutionId);
        logMap.put("versionNo", nextVerNo);
        logMap.put("publisher", operator);
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_PUBLISH, TARGET_TYPE, version.getId(), null, logMap);

        invalidateSolutionCache(solutionId);

        log.info("publish solution success solutionId={} versionNo={}", solutionId, nextVerNo);
        return toVersionVO(version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<IndustrySolutionVersionVO> listVersions(Long solutionId) {
        requireSolutionExists(solutionId);
        List<IndustrySolutionVersionEntity> list = versionMapper.selectList(
                new LambdaQueryWrapper<IndustrySolutionVersionEntity>()
                        .eq(IndustrySolutionVersionEntity::getSolutionId, solutionId)
                        .orderByDesc(IndustrySolutionVersionEntity::getVersionNo)
        );
        return list.stream().map(this::toVersionVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    protected IndustrySolutionVersionVO doRollback(Long solutionId, Long targetVersionId, DetailRollbackDTO rollbackDTO, String operator) {

        IndustrySolutionEntity solution = requireSolutionExists(solutionId);
        IndustrySolutionDraftEntity currentDraft = queryDraftBySolutionId(solutionId);
        if (currentDraft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "草稿不存在，无法回滚");
        }
        ConcurrencyHelper.assertVersion(currentDraft.getVersion(), rollbackDTO.getVersion());
        contentReferenceGuard.assertNotReferencedByPage("industry_solution", "IndustrySolution", solutionId);
        IndustrySolutionVersionEntity targetVersion = versionMapper.selectById(targetVersionId);
        if (targetVersion == null || !targetVersion.getSolutionId().equals(solutionId)) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "指定的历史发布版本不存在");
        }

        int nextVerNo = getNextVersionNo(solutionId);

        IndustrySolutionVersionEntity rollbackVersion = new IndustrySolutionVersionEntity();
        rollbackVersion.setSolutionId(solutionId);
        rollbackVersion.setVersionNo(nextVerNo);
        rollbackVersion.setSnapshotJson(targetVersion.getSnapshotJson());
        rollbackVersion.setSnapshotHash(targetVersion.getSnapshotHash());
        rollbackVersion.setChangeSummary("回滚至版本 No." + targetVersion.getVersionNo() + (rollbackDTO.getChangeSummary() != null ? " (" + rollbackDTO.getChangeSummary() + ")" : ""));
        rollbackVersion.setPublisher(operator);
        rollbackVersion.setRollbackSourceVersionId(targetVersion.getId());
        rollbackVersion.setPublishedAt(LocalDateTime.now());
        versionMapper.insert(rollbackVersion);
        mediaAssetService.bindPublishedSnapshotMedia(
                "INDUSTRY_SOLUTION_VERSION", rollbackVersion.getId(), rollbackVersion.getSnapshotJson());

        IndustrySolutionDraftEntity draft = queryDraftBySolutionId(solutionId);
        if (draft != null) {
            draft.setDraftJson(targetVersion.getSnapshotJson());
            draft.setDraftHash(targetVersion.getSnapshotHash());
            draft.setEditorSessionRemark("版本回滚自动覆盖草稿");
            draft.setUpdatedBy(operator);
            draftMapper.updateById(draft);
        }

        solution.setVisible(true);
        solutionMapper.updateById(solution);

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("solutionId", solutionId);
        logMap.put("targetVersionId", targetVersionId);
        logMap.put("newVersionNo", nextVerNo);
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_ROLLBACK, TARGET_TYPE, rollbackVersion.getId(), null, logMap);

        invalidateSolutionCache(solutionId);

        log.info("rollback solution success solutionId={} targetVersionId={} newVersionNo={}", solutionId, targetVersionId, nextVerNo);
        return toVersionVO(rollbackVersion);
    }

    @Override
    @Transactional
    protected void doOffline(Long solutionId, DetailOfflineDTO offlineDTO, String operator) {

        IndustrySolutionEntity solution = requireSolutionExists(solutionId);
        IndustrySolutionDraftEntity draft = queryDraftBySolutionId(solutionId);
        if (draft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "草稿不存在，无法下线");
        }
        ConcurrencyHelper.assertVersion(draft.getVersion(), offlineDTO.getVersion());

        // 校验已发布 ACTIVE 页面强引用拦截
        contentReferenceGuard.assertNotReferencedByPage("industry_solution", "IndustrySolution", solutionId);

        solution.setVisible(false);
        solutionMapper.updateById(solution);

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("solutionId", solutionId);
        logMap.put("reason", offlineDTO.getReason());
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_OFFLINE, TARGET_TYPE, solutionId, null, logMap);

        invalidateSolutionCache(solutionId);

        log.info("offline solution success solutionId={} reason={}", solutionId, offlineDTO.getReason());
    }

    @Override
    @Transactional(readOnly = true)
    public PortalIndustrySolutionDetailVO getPortalSolutionDetail(Long solutionId) {
        IndustrySolutionEntity solution = solutionMapper.selectById(solutionId);
        if (solution == null || Boolean.FALSE.equals(solution.getVisible())) {
            throw new BusinessException(ErrorCode.PRODUCT_SOLUTION_NOT_FOUND, "行业解决方案不存在或暂未上线");
        }

        // 查询最新的正式发布版本快照
        List<IndustrySolutionVersionEntity> versions = versionMapper.selectList(
                new LambdaQueryWrapper<IndustrySolutionVersionEntity>()
                        .eq(IndustrySolutionVersionEntity::getSolutionId, solutionId)
                        .orderByDesc(IndustrySolutionVersionEntity::getVersionNo)
        );
        if (versions.isEmpty()) {
            throw new BusinessException(ErrorCode.PRODUCT_SOLUTION_NOT_FOUND, "行业解决方案暂未发布上线");
        }
        IndustrySolutionVersionEntity activeVersion = versions.get(0);

        JsonNode snapshot;
        try {
            snapshot = objectMapper.readTree(activeVersion.getSnapshotJson());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "行业解决方案发布快照格式无效", e);
        }
        Long iconMediaId = longValue(snapshot, "iconMediaId");
        String iconUrl = iconMediaId == null ? null : mediaAssetService.requireUsableImage(iconMediaId).getPublicUrl();
        return new PortalIndustrySolutionDetailVO(
                solutionId,
                textValue(snapshot, "name"),
                iconUrl,
                textValue(snapshot, "description"),
                readStringList(snapshot.get("customerTags")),
                objectMapper.convertValue(snapshot, Object.class)
        );
    }

    private IndustrySolutionEntity requireSolutionExists(Long solutionId) {
        IndustrySolutionEntity entity = solutionMapper.selectById(solutionId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "未找到该行业解决方案");
        }
        return entity;
    }

    private IndustrySolutionDraftEntity queryDraftBySolutionId(Long solutionId) {
        return draftMapper.selectOne(
                new LambdaQueryWrapper<IndustrySolutionDraftEntity>().eq(IndustrySolutionDraftEntity::getSolutionId, solutionId)
        );
    }

    private int getNextVersionNo(Long solutionId) {
        List<IndustrySolutionVersionEntity> list = versionMapper.selectList(
                new LambdaQueryWrapper<IndustrySolutionVersionEntity>()
                        .eq(IndustrySolutionVersionEntity::getSolutionId, solutionId)
                        .orderByDesc(IndustrySolutionVersionEntity::getVersionNo)
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

    private IndustrySolutionDraftVO toDraftVO(IndustrySolutionDraftEntity entity) {
        if (entity == null) {
            return null;
        }
        Object draftObj;
        try {
            draftObj = objectMapper.readValue(entity.getDraftJson(), Object.class);
        } catch (Exception e) {
            draftObj = entity.getDraftJson();
        }
        return new IndustrySolutionDraftVO(
                entity.getId(), entity.getSolutionId(), draftObj, entity.getDraftHash(),
                entity.getEditorSessionRemark(), entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private IndustrySolutionVersionVO toVersionVO(IndustrySolutionVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        Object snapshotObj;
        try {
            snapshotObj = objectMapper.readValue(entity.getSnapshotJson(), Object.class);
        } catch (Exception e) {
            snapshotObj = entity.getSnapshotJson();
        }
        return new IndustrySolutionVersionVO(
                entity.getId(), entity.getSolutionId(), entity.getVersionNo(), snapshotObj,
                entity.getSnapshotHash(), entity.getChangeSummary(), entity.getPublisher(),
                entity.getRollbackSourceVersionId(), entity.getPublishedAt(), entity.getCreatedAt()
        );
    }

    private void invalidateSolutionCache(Long solutionId) {
        portalCacheSupport.invalidate(portalCacheSupport.buildKey("industry_solutions"));
        pageCacheInvalidationService.invalidateCacheByTarget("industry_solution", "IndustrySolution", String.valueOf(solutionId));
    }

    /** 对行业方案草稿执行富文本、媒体、SEO、链接与关联资源校验。 */
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
                    if (normalized.contains("content") || normalized.contains("richtext")
                            || normalized.contains("html") || normalized.equals("body")) {
                        objectNode.put(field, detailValidationSupport.cleanRichTextHtml(value.asText()));
                    }
                    if (normalized.endsWith("link") || normalized.endsWith("url")) {
                        detailValidationSupport.validateLinkProtocol(value.asText());
                    }
                }
                if (value.isNumber()) {
                    validateReferencedId(normalized, value.asLong());
                    if (isMediaIdField(normalized)) {
                        detailValidationSupport.validateMediaUsable(value.asLong());
                    }
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
                    validateReferencedId(parentField, item.asLong());
                    if (isMediaIdField(parentField)) {
                        detailValidationSupport.validateMediaUsable(item.asLong());
                    }
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
                || field.equals("iconmediaid") || field.equals("imageid") || field.equals("imageids")
                || field.equals("covermediaid") || field.equals("thumbnailid") || field.equals("thumbnailids"));
    }

    private void validateReferencedId(String field, Long id) {
        if (field == null || (!field.endsWith("id") && !field.endsWith("ids"))) {
            return;
        }
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "关联资源 ID 必须为正数");
        }
        if (field.equals("productid") || field.equals("productids") || field.equals("relatedproductids")) {
            if (productMapper.selectById(id) == null) {
                throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "关联产品不存在或已删除: " + id);
            }
        }
        if (field.equals("caseid") || field.equals("caseids") || field.equals("relatedcaseids")
                || field.equals("recommendedcaseids")) {
            CaseEntity caseEntity = caseMapper.selectById(id);
            if (caseEntity == null) {
                throw new BusinessException(ErrorCode.CASE_NOT_FOUND, "关联案例不存在或已删除: " + id);
            }
        }
        if (field.equals("industrysolutionid") || field.equals("industrysolutionids")
                || field.equals("relatedindustrysolutionids")) {
            if (solutionMapper.selectById(id) == null) {
                throw new BusinessException(ErrorCode.PRODUCT_SOLUTION_NOT_FOUND, "关联行业方案不存在或已删除: " + id);
            }
        }
    }

    private String textValue(JsonNode objectNode, String field) {
        JsonNode value = objectNode.get(field);
        return value != null && value.isTextual() ? value.asText() : null;
    }

    private List<String> readStringList(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return java.util.stream.StreamSupport.stream(node.spliterator(), false)
                .filter(JsonNode::isTextual)
                .map(JsonNode::asText)
                .toList();
    }

    private Long longValue(JsonNode objectNode, String field) {
        JsonNode value = objectNode.get(field);
        return value != null && value.canConvertToLong() ? value.asLong() : null;
    }
}
