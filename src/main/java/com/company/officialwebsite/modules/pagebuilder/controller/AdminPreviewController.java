package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.utils.DataMaskUtils;
import com.company.officialwebsite.modules.pagebuilder.dto.PreviewCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftService;
import com.company.officialwebsite.modules.pagebuilder.service.PreviewTokenService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PreviewCreateVO;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminPreviewController：后台受控预览 Token 管理接口。
 * <p>
 * 提供预览 Token 的生成（绑定草稿 pageId/schemaHash/操作人）和主动撤销能力。
 * 所有接口需持有 {@code ADMINISTRATOR} 角色。
 * 日志中 Token 均通过 {@link DataMaskUtils#maskPreviewToken} 脱敏，禁止记录完整 Token。
 * </p>
 */
@RestController
@RequestMapping("/admin/api/page-builder")
public class AdminPreviewController {

    private static final Logger log = LoggerFactory.getLogger(AdminPreviewController.class);

    private final PageDraftService pageDraftService;
    private final PreviewTokenService previewTokenService;
    private final OfficialProperties officialProperties;

    public AdminPreviewController(
            PageDraftService pageDraftService,
            PreviewTokenService previewTokenService,
            OfficialProperties officialProperties) {
        this.pageDraftService = pageDraftService;
        this.previewTokenService = previewTokenService;
        this.officialProperties = officialProperties;
    }

    /**
     * 生成受控预览 Token。
     * <p>
     * 若请求体中携带 schemaHash，则与数据库当前草稿哈希比对；
     * 不一致时返回 {@link ErrorCode#PAGE_PREVIEW_SCHEMA_HASH_MISMATCH}，提示前端先保存草稿。
     * </p>
     *
     * @param pageId    页面定义 ID
     * @param dto       可选的 schemaHash 校验请求体
     * @param principal 当前登录管理员
     * @return 含 previewToken、previewUrl 和 expiresAt 的响应
     */
    @PostMapping("/drafts/{pageId}/previews")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PreviewCreateVO> createPreview(
            @PathVariable Long pageId,
            @Valid @RequestBody(required = false) PreviewCreateDTO dto,
            @AuthenticationPrincipal AdminUserPrincipal principal) {

        log.info("admin create preview pageId={} operator={}", pageId, principal.getUsername());

        // 加载草稿（不存在时 getDraft 内部抛 PAGE_DRAFT_NOT_FOUND）
        PageDraftVO draft = pageDraftService.getDraft(pageId);

        // 若草稿 Schema 为空，不允许生成预览
        if (draft.getSchemaJson() == null) {
            throw new BusinessException(ErrorCode.PAGE_DRAFT_NOT_FOUND, "草稿内容为空，请先保存内容后再生成预览");
        }

        // 可选：比对前端传入的 schemaHash 与数据库中的一致性
        if (dto != null && StringUtils.hasText(dto.getSchemaHash())) {
            String serverHash = draft.getSchemaHash();
            if (!dto.getSchemaHash().trim().equals(serverHash)) {
                log.warn("preview schemaHash mismatch pageId={} clientHash={} serverHash={}",
                        pageId, dto.getSchemaHash(), serverHash);
                throw new BusinessException(ErrorCode.PAGE_PREVIEW_SCHEMA_HASH_MISMATCH);
            }
        }

        String token = previewTokenService.createToken(
                pageId,
                draft.getSchemaHash(),
                principal.getUsername()
        );

        String previewUrl = officialProperties.getPageBuilder().buildPreviewUrl(token);
        PreviewCreateVO vo = new PreviewCreateVO(token, previewUrl, previewTokenService.computeExpiresAt());

        log.info("admin preview created pageId={} token={}", pageId, DataMaskUtils.maskPreviewToken(token));
        return ApiResponse.success(vo);
    }

    /**
     * 主动撤销预览 Token，使预览链接立即失效。
     * Token 不存在时静默成功（幂等），不抛出异常。
     *
     * @param previewToken 要撤销的预览 Token
     */
    @DeleteMapping("/previews/{previewToken}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> revokePreview(
            @PathVariable String previewToken,
            @AuthenticationPrincipal AdminUserPrincipal principal) {

        log.info("admin revoke preview token={} operator={}",
                DataMaskUtils.maskPreviewToken(previewToken), principal.getUsername());
        previewTokenService.revokeToken(previewToken);
        return ApiResponse.success();
    }
}
