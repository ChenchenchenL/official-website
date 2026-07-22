package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.modules.pagebuilder.converter.PageDraftConverter;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.PublishSnapshotStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftService;
import com.company.officialwebsite.modules.pagebuilder.service.PageSchemaValidationService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * PageDraftServiceImpl：页面草稿管理业务逻辑实现。
 * <p>
 * 读操作标注 {@code readOnly=true} 事务；写操作标注完整事务。
 * 预览 Token 生命周期管理已完整迁移至 {@link com.company.officialwebsite.modules.pagebuilder.service.PreviewTokenService}。
 * </p>
 */
@Service
public class PageDraftServiceImpl implements PageDraftService {

    private static final Logger log = LoggerFactory.getLogger(PageDraftServiceImpl.class);

    private static final String MODULE_NAME = "PAGE_BUILDER";
    private static final String ACTION_SAVE_DRAFT = "SAVE_DRAFT";
    private static final String ACTION_RESET_DRAFT = "RESET_DRAFT_TO_PUBLISHED";
    private static final String TARGET_TYPE = "PAGE_DRAFT";

    private final PageDraftMapper draftMapper;
    private final PagePublishSnapshotMapper pagePublishSnapshotMapper;
    private final PageDefinitionMapper pageDefinitionMapper;
    private final PageDraftConverter draftConverter;
    private final PageSchemaValidationService pageSchemaValidationService;
    private final EditorLockService editorLockService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public PageDraftServiceImpl(
            PageDraftMapper draftMapper,
            PagePublishSnapshotMapper pagePublishSnapshotMapper,
            PageDefinitionMapper pageDefinitionMapper,
            PageDraftConverter draftConverter,
            PageSchemaValidationService pageSchemaValidationService,
            EditorLockService editorLockService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.draftMapper = draftMapper;
        this.pagePublishSnapshotMapper = pagePublishSnapshotMapper;
        this.pageDefinitionMapper = pageDefinitionMapper;
        this.draftConverter = draftConverter;
        this.pageSchemaValidationService = pageSchemaValidationService;
        this.editorLockService = editorLockService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageDraftVO getDraft(Long pageId) {
        requireActivePage(pageId);
        PageDraftEntity draft = queryDraftByPageId(pageId);
        if (draft == null) {
            log.warn("getDraft pageId={} draft not found", pageId);
            throw new BusinessException(ErrorCode.PAGE_DRAFT_NOT_FOUND);
        }
        log.info("getDraft pageId={} draftId={}", pageId, draft.getId());
        return draftConverter.toVO(draft);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PageDraftVO saveDraft(Long pageId, PageDraftSaveDTO dto, String lockToken, String operator) {
        // 门禁：独占编辑锁校验
        editorLockService.validateLock(
                com.company.officialwebsite.common.enums.EditorResourceTypeEnum.PAGE,
                pageId, lockToken, operator);

        requireActivePage(pageId);

        PageDraftEntity draft = queryDraftByPageId(pageId);
        if (draft == null) {
            log.warn("saveDraft pageId={} draft not found", pageId);
            throw new BusinessException(ErrorCode.PAGE_DRAFT_NOT_FOUND);
        }

        // 乐观锁版本校验：若版本冲突，抛出包含最新草稿 VO 恢复数据的 BusinessException
        if (dto.getVersion() == null || dto.getVersion() < 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "版本号不能为负数");
        }
        if (!draft.getVersion().equals(dto.getVersion())) {
            log.warn("saveDraft version conflict pageId={} clientVer={} dbVer={}", pageId, dto.getVersion(), draft.getVersion());
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "草稿版本冲突，请刷新后重试", draftConverter.toVO(draft));
        }

        // Schema 合规性校验（白名单、XSS 清洗等）
        pageSchemaValidationService.validateSchema(dto.getSchemaJson());

        // 计算 SHA-256 哈希
        String schemaHash = computeSchemaHash(dto.getSchemaJson());

        // 记录更新前快照
        PageDraftVO beforeSnapshot = draftConverter.toVO(draft);

        // 更新实体字段
        draft.setSchemaJson(dto.getSchemaJson());
        draft.setSchemaHash(schemaHash);
        draft.setEditorSessionRemark(dto.getEditorSessionRemark());

        ConcurrencyHelper.tryUpdate(draftMapper, draft);

        // 审计日志
        PageDraftVO afterSnapshot = draftConverter.toVO(draft);
        auditLogService.recordGenericOperation(
                MODULE_NAME, ACTION_SAVE_DRAFT, TARGET_TYPE, draft.getId(),
                beforeSnapshot, afterSnapshot);

        log.info("saveDraft success pageId={} draftId={} schemaHash={}", pageId, draft.getId(), schemaHash);

        return afterSnapshot;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public PageDraftVO resetDraftToPublished(Long pageId, String lockToken, String operator) {
        // 门禁：独占编辑锁校验
        editorLockService.validateLock(
                com.company.officialwebsite.common.enums.EditorResourceTypeEnum.PAGE,
                pageId, lockToken, operator);

        requireActivePage(pageId);

        PageDraftEntity draft = queryDraftByPageId(pageId);
        if (draft == null) {
            log.warn("resetDraftToPublished failed: pageId={} draft not found", pageId);
            throw new BusinessException(ErrorCode.PAGE_DRAFT_NOT_FOUND);
        }

        // 查询当前在线 ACTIVE 快照
        PagePublishSnapshotEntity activeSnapshot = pagePublishSnapshotMapper.selectOne(
                new LambdaQueryWrapper<PagePublishSnapshotEntity>()
                        .eq(PagePublishSnapshotEntity::getPageId, pageId)
                        .eq(PagePublishSnapshotEntity::getPublishStatus, PublishSnapshotStatusEnum.ACTIVE.name())
        );

        if (activeSnapshot == null || activeSnapshot.getSnapshotJson() == null) {
            log.warn("resetDraftToPublished failed: pageId={} active snapshot not found", pageId);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "页面尚未发布过，不存在可恢复的已发布版本");
        }

        // 幂等性校验：若草稿已经与 ACTIVE 快照相同，无需重复数据库更新
        if (draft.getSchemaHash() != null && draft.getSchemaHash().equals(activeSnapshot.getSnapshotHash())) {
            log.info("resetDraftToPublished: draft already identical to ACTIVE snapshot pageId={}", pageId);
            return draftConverter.toVO(draft);
        }

        PageDraftVO beforeSnapshot = draftConverter.toVO(draft);

        draft.setSchemaJson(activeSnapshot.getSnapshotJson());
        draft.setSchemaHash(activeSnapshot.getSnapshotHash());
        draft.setEditorSessionRemark("重置草稿为当前已发布版本");

        ConcurrencyHelper.tryUpdate(draftMapper, draft);

        PageDraftVO afterSnapshot = draftConverter.toVO(draft);
        auditLogService.recordGenericOperation(
                MODULE_NAME, ACTION_RESET_DRAFT, TARGET_TYPE, draft.getId(),
                beforeSnapshot, afterSnapshot);

        log.info("resetDraftToPublished success pageId={} draftId={}", pageId, draft.getId());
        return getDraft(pageId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private PageDefinitionEntity requireActivePage(Long id) {
        PageDefinitionEntity entity = pageDefinitionMapper.selectById(id);
        if (entity == null) {
            log.warn("Page definition not found or deleted: id={}", id);
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND);
        }
        return entity;
    }

    /**
     * 按 pageId 查询未逻辑删除的草稿实体。
     */
    private PageDraftEntity queryDraftByPageId(Long pageId) {
        return draftMapper.selectOne(
                new LambdaQueryWrapper<PageDraftEntity>()
                        .eq(PageDraftEntity::getPageId, pageId)
        );
    }

    /**
     * 将 Schema 模型序列化为 JSON 字符串并计算 SHA-256 哈希（16 进制小写）。
     */
    private String computeSchemaHash(PageSchemaModel schema) {
        try {
            String json = objectMapper.writeValueAsString(schema);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 是 JVM 必须支持的算法，此分支在正常运行环境不会触发
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "哈希算法不可用", e);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Schema 序列化失败", e);
        }
    }
}

