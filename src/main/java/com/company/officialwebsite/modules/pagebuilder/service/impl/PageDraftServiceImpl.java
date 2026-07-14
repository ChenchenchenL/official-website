package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.modules.pagebuilder.converter.PageDraftConverter;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
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
    private static final String TARGET_TYPE = "PAGE_DRAFT";

    private final PageDraftMapper draftMapper;
    private final PageDraftConverter draftConverter;
    private final PageSchemaValidationService pageSchemaValidationService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public PageDraftServiceImpl(
            PageDraftMapper draftMapper,
            PageDraftConverter draftConverter,
            PageSchemaValidationService pageSchemaValidationService,
            AuditLogService auditLogService,
            ObjectMapper objectMapper) {
        this.draftMapper = draftMapper;
        this.draftConverter = draftConverter;
        this.pageSchemaValidationService = pageSchemaValidationService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageDraftVO getDraft(Long pageId) {
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
    public PageDraftVO saveDraft(Long pageId, PageDraftSaveDTO dto) {
        PageDraftEntity draft = queryDraftByPageId(pageId);
        if (draft == null) {
            log.warn("saveDraft pageId={} draft not found", pageId);
            throw new BusinessException(ErrorCode.PAGE_DRAFT_NOT_FOUND);
        }

        // 乐观锁版本校验
        ConcurrencyHelper.assertVersion(draft.getVersion(), dto.getVersion());

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

        // 重新查询以返回最新状态（含 updatedAt 等自动填充字段）
        return getDraft(pageId);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

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

