package com.company.officialwebsite.modules.casecenter.service.impl;

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
import com.company.officialwebsite.modules.casecenter.entity.CaseDraftEntity;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.entity.CaseVersionEntity;
import com.company.officialwebsite.modules.casecenter.mapper.CaseDraftMapper;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.casecenter.mapper.CaseVersionMapper;
import com.company.officialwebsite.modules.casecenter.service.CaseEditorService;
import com.company.officialwebsite.modules.casecenter.vo.CaseDraftVO;
import com.company.officialwebsite.modules.casecenter.vo.CaseVersionVO;
import com.company.officialwebsite.modules.content.dto.DetailPreviewTokenData;
import com.company.officialwebsite.modules.content.service.ContentReferenceGuard;
import com.company.officialwebsite.modules.content.service.DetailPreviewTokenService;
import com.company.officialwebsite.modules.content.service.DetailValidationSupport;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * CaseEditorServiceImpl：标杆案例详情编辑全生命周期业务实现类。
 */
@Service
public class CaseEditorServiceImpl implements CaseEditorService {

    private static final Logger log = LoggerFactory.getLogger(CaseEditorServiceImpl.class);

    private static final String BIZ_MODULE = "CASE";
    private static final String TARGET_TYPE = "CASE_DETAIL";
    private static final String ACTION_SAVE_DRAFT = "SAVE_CASE_DRAFT";
    private static final String ACTION_PUBLISH = "PUBLISH_CASE";
    private static final String ACTION_ROLLBACK = "ROLLBACK_CASE";
    private static final String ACTION_OFFLINE = "OFFLINE_CASE";

    private final CaseMapper caseMapper;
    private final CaseDraftMapper draftMapper;
    private final CaseVersionMapper versionMapper;
    private final EditorLockService editorLockService;
    private final DetailPreviewTokenService detailPreviewTokenService;
    private final DetailValidationSupport detailValidationSupport;
    private final ContentReferenceGuard contentReferenceGuard;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final ObjectMapper objectMapper;

    public CaseEditorServiceImpl(
            CaseMapper caseMapper,
            CaseDraftMapper draftMapper,
            CaseVersionMapper versionMapper,
            EditorLockService editorLockService,
            DetailPreviewTokenService detailPreviewTokenService,
            DetailValidationSupport detailValidationSupport,
            ContentReferenceGuard contentReferenceGuard,
            AuditLogService auditLogService,
            PortalCacheSupport portalCacheSupport,
            ObjectMapper objectMapper) {
        this.caseMapper = caseMapper;
        this.draftMapper = draftMapper;
        this.versionMapper = versionMapper;
        this.editorLockService = editorLockService;
        this.detailPreviewTokenService = detailPreviewTokenService;
        this.detailValidationSupport = detailValidationSupport;
        this.contentReferenceGuard = contentReferenceGuard;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public CaseDraftVO createDraftShell(String operator) {
        CaseEntity caseEntity = new CaseEntity();
        caseEntity.setTitle("未命名案例_" + System.currentTimeMillis());
        caseEntity.setLogoMediaId(0L);
        caseEntity.setSummary("暂无摘要");
        caseEntity.setStatus("DRAFT");
        caseEntity.setVisible(false);
        caseEntity.setSortOrder(100);
        caseMapper.insert(caseEntity);

        CaseDraftEntity draft = new CaseDraftEntity();
        draft.setCaseId(caseEntity.getId());
        draft.setDraftJson("{\"title\":\"未命名案例\"}");
        draft.setDraftHash(computeHash(draft.getDraftJson()));
        draft.setCreatedBy(operator);
        draft.setUpdatedBy(operator);
        draftMapper.insert(draft);

        log.info("createDraftShell case success caseId={} draftId={}", caseEntity.getId(), draft.getId());
        return toDraftVO(draft);
    }

    @Override
    @Transactional(readOnly = true)
    public CaseDraftVO getDraft(Long caseId) {
        requireCaseExists(caseId);
        CaseDraftEntity draft = queryDraftByCaseId(caseId);
        if (draft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "该案例的详情草稿不存在");
        }
        return toDraftVO(draft);
    }

    @Override
    @Transactional
    public CaseDraftVO saveDraft(Long caseId, DetailDraftSaveDTO saveDTO, String lockToken, String operator) {
        // 独占编辑锁校验
        editorLockService.validateLock(EditorResourceTypeEnum.CASE, caseId, lockToken, operator);

        CaseEntity caseEntity = requireCaseExists(caseId);
        CaseDraftEntity draft = queryDraftByCaseId(caseId);
        if (draft == null) {
            draft = new CaseDraftEntity();
            draft.setCaseId(caseId);
            draft.setVersion(0);
        } else {
            ConcurrencyHelper.assertVersion(draft.getVersion(), saveDTO.getVersion());
        }

        String jsonStr;
        try {
            jsonStr = objectMapper.writeValueAsString(saveDTO.getDraft());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "草稿 JSON 格式无效", e);
        }

        String cleanedJsonStr = jsonStr;
        String hash = computeHash(cleanedJsonStr);

        CaseDraftVO beforeSnapshot = toDraftVO(draft);

        draft.setDraftJson(cleanedJsonStr);
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

        CaseDraftVO afterSnapshot = toDraftVO(draft);
        auditLogService.recordGenericOperation(
                BIZ_MODULE, ACTION_SAVE_DRAFT, TARGET_TYPE, draft.getId(), beforeSnapshot, afterSnapshot);

        log.info("saveDraft case success caseId={} draftId={} hash={}", caseId, draft.getId(), hash);
        return getDraft(caseId);
    }

    @Override
    @Transactional(readOnly = true)
    public String createPreviewToken(Long caseId, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.CASE, caseId, lockToken, operator);
        CaseDraftEntity draft = queryDraftByCaseId(caseId);
        if (draft == null || draft.getDraftJson() == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "草稿内容为空，无法生成预览");
        }

        return detailPreviewTokenService.createToken(
                EditorResourceTypeEnum.CASE, caseId, draft.getDraftHash(), operator);
    }

    @Override
    @Transactional(readOnly = true)
    public Object renderPreview(Long caseId, String previewToken, String operator) {
        DetailPreviewTokenData tokenData = detailPreviewTokenService.resolveToken(previewToken);
        if (!EditorResourceTypeEnum.CASE.equals(tokenData.getResourceType()) || !caseId.equals(tokenData.getResourceId())) {
            throw new BusinessException(ErrorCode.DETAIL_PREVIEW_TOKEN_EXPIRED, "预览 Token 与当前案例不匹配");
        }
        if (tokenData.getCreatedBy() != null && !tokenData.getCreatedBy().equalsIgnoreCase(operator)) {
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "仅限创建该预览链接的管理员访问");
        }

        CaseDraftEntity draft = queryDraftByCaseId(caseId);
        if (draft == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "该案例的详情草稿不存在");
        }
        try {
            return objectMapper.readValue(draft.getDraftJson(), Object.class);
        } catch (Exception e) {
            return draft.getDraftJson();
        }
    }

    @Override
    @Transactional
    public void revokePreviewToken(Long caseId, String previewToken, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.CASE, caseId, lockToken, operator);
        detailPreviewTokenService.revokeToken(previewToken);
    }

    @Override
    @Transactional
    public CaseVersionVO publish(Long caseId, DetailPublishDTO publishDTO, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.CASE, caseId, lockToken, operator);

        CaseEntity caseEntity = requireCaseExists(caseId);
        CaseDraftEntity draft = queryDraftByCaseId(caseId);
        if (draft == null || draft.getDraftJson() == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "草稿不存在或内容为空，无法发布");
        }

        int nextVerNo = getNextVersionNo(caseId);

        CaseVersionEntity version = new CaseVersionEntity();
        version.setCaseId(caseId);
        version.setVersionNo(nextVerNo);
        version.setSnapshotJson(draft.getDraftJson());
        version.setSnapshotHash(draft.getDraftHash());
        version.setChangeSummary(publishDTO.getChangeSummary().trim());
        version.setPublisher(operator);
        version.setPublishedAt(LocalDateTime.now());
        versionMapper.insert(version);

        caseEntity.setStatus("PUBLISHED");
        caseEntity.setVisible(true);
        caseMapper.updateById(caseEntity);

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("caseId", caseId);
        logMap.put("versionNo", nextVerNo);
        logMap.put("publisher", operator);
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_PUBLISH, TARGET_TYPE, version.getId(), null, logMap);

        invalidateCaseCache(caseId);

        log.info("publish case success caseId={} versionNo={}", caseId, nextVerNo);
        return toVersionVO(version);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CaseVersionVO> listVersions(Long caseId) {
        requireCaseExists(caseId);
        List<CaseVersionEntity> list = versionMapper.selectList(
                new LambdaQueryWrapper<CaseVersionEntity>()
                        .eq(CaseVersionEntity::getCaseId, caseId)
                        .orderByDesc(CaseVersionEntity::getVersionNo)
        );
        return list.stream().map(this::toVersionVO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CaseVersionVO rollback(Long caseId, Long targetVersionId, DetailRollbackDTO rollbackDTO, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.CASE, caseId, lockToken, operator);

        CaseEntity caseEntity = requireCaseExists(caseId);
        CaseVersionEntity targetVersion = versionMapper.selectById(targetVersionId);
        if (targetVersion == null || !targetVersion.getCaseId().equals(caseId)) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "指定的历史发布版本不存在");
        }

        int nextVerNo = getNextVersionNo(caseId);

        CaseVersionEntity rollbackVersion = new CaseVersionEntity();
        rollbackVersion.setCaseId(caseId);
        rollbackVersion.setVersionNo(nextVerNo);
        rollbackVersion.setSnapshotJson(targetVersion.getSnapshotJson());
        rollbackVersion.setSnapshotHash(targetVersion.getSnapshotHash());
        rollbackVersion.setChangeSummary("回滚至版本 No." + targetVersion.getVersionNo() + (rollbackDTO.getChangeSummary() != null ? " (" + rollbackDTO.getChangeSummary() + ")" : ""));
        rollbackVersion.setPublisher(operator);
        rollbackVersion.setRollbackSourceVersionId(targetVersion.getId());
        rollbackVersion.setPublishedAt(LocalDateTime.now());
        versionMapper.insert(rollbackVersion);

        CaseDraftEntity draft = queryDraftByCaseId(caseId);
        if (draft != null) {
            draft.setDraftJson(targetVersion.getSnapshotJson());
            draft.setDraftHash(targetVersion.getSnapshotHash());
            draft.setEditorSessionRemark("版本回滚自动覆盖草稿");
            draft.setUpdatedBy(operator);
            draftMapper.updateById(draft);
        }

        caseEntity.setStatus("PUBLISHED");
        caseEntity.setVisible(true);
        caseMapper.updateById(caseEntity);

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("caseId", caseId);
        logMap.put("targetVersionId", targetVersionId);
        logMap.put("newVersionNo", nextVerNo);
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_ROLLBACK, TARGET_TYPE, rollbackVersion.getId(), null, logMap);

        invalidateCaseCache(caseId);

        log.info("rollback case success caseId={} targetVersionId={} newVersionNo={}", caseId, targetVersionId, nextVerNo);
        return toVersionVO(rollbackVersion);
    }

    @Override
    @Transactional
    public void offline(Long caseId, DetailOfflineDTO offlineDTO, String lockToken, String operator) {
        editorLockService.validateLock(EditorResourceTypeEnum.CASE, caseId, lockToken, operator);

        CaseEntity caseEntity = requireCaseExists(caseId);

        // 校验已发布 ACTIVE 页面强引用拦截
        contentReferenceGuard.assertNotReferencedByPage("case", "Case", caseId);

        caseEntity.setStatus("OFFLINE");
        caseEntity.setVisible(false);
        caseMapper.updateById(caseEntity);

        Map<String, Object> logMap = new LinkedHashMap<>();
        logMap.put("caseId", caseId);
        logMap.put("reason", offlineDTO.getReason());
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_OFFLINE, TARGET_TYPE, caseId, null, logMap);

        invalidateCaseCache(caseId);

        log.info("offline case success caseId={} reason={}", caseId, offlineDTO.getReason());
    }

    private CaseEntity requireCaseExists(Long caseId) {
        CaseEntity entity = caseMapper.selectById(caseId);
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "未找到该标杆案例");
        }
        return entity;
    }

    private CaseDraftEntity queryDraftByCaseId(Long caseId) {
        return draftMapper.selectOne(
                new LambdaQueryWrapper<CaseDraftEntity>().eq(CaseDraftEntity::getCaseId, caseId)
        );
    }

    private int getNextVersionNo(Long caseId) {
        List<CaseVersionEntity> list = versionMapper.selectList(
                new LambdaQueryWrapper<CaseVersionEntity>()
                        .eq(CaseVersionEntity::getCaseId, caseId)
                        .orderByDesc(CaseVersionEntity::getVersionNo)
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

    private CaseDraftVO toDraftVO(CaseDraftEntity entity) {
        if (entity == null) {
            return null;
        }
        Object draftObj;
        try {
            draftObj = objectMapper.readValue(entity.getDraftJson(), Object.class);
        } catch (Exception e) {
            draftObj = entity.getDraftJson();
        }
        return new CaseDraftVO(
                entity.getId(), entity.getCaseId(), draftObj, entity.getDraftHash(),
                entity.getEditorSessionRemark(), entity.getVersion(), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private CaseVersionVO toVersionVO(CaseVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        Object snapshotObj;
        try {
            snapshotObj = objectMapper.readValue(entity.getSnapshotJson(), Object.class);
        } catch (Exception e) {
            snapshotObj = entity.getSnapshotJson();
        }
        return new CaseVersionVO(
                entity.getId(), entity.getCaseId(), entity.getVersionNo(), snapshotObj,
                entity.getSnapshotHash(), entity.getChangeSummary(), entity.getPublisher(),
                entity.getRollbackSourceVersionId(), entity.getPublishedAt(), entity.getCreatedAt()
        );
    }

    private void invalidateCaseCache(Long caseId) {
        String detailCacheKey = portalCacheSupport.buildKey("case", "detail", String.valueOf(caseId));
        String listCacheKey = portalCacheSupport.buildKey("case", "list");
        portalCacheSupport.invalidate(detailCacheKey, listCacheKey);
    }
}
