package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.utils.DataMaskUtils;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.pagebuilder.dto.PreviewCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.model.PreviewTokenData;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftService;
import com.company.officialwebsite.modules.pagebuilder.service.PreviewTokenService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PreviewCreateVO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminPreviewController：后台受控预览 Token 管理接口。
 */
@RestController
@RequestMapping("/admin/api/page-builder")
public class AdminPreviewController {

    private static final Logger log = LoggerFactory.getLogger(AdminPreviewController.class);

    private final PageDraftService pageDraftService;
    private final PreviewTokenService previewTokenService;
    private final EditorLockService editorLockService;
    private final OfficialProperties officialProperties;

    public AdminPreviewController(
            PageDraftService pageDraftService,
            PreviewTokenService previewTokenService,
            EditorLockService editorLockService,
            OfficialProperties officialProperties) {
        this.pageDraftService = pageDraftService;
        this.previewTokenService = previewTokenService;
        this.editorLockService = editorLockService;
        this.officialProperties = officialProperties;
    }

    /**
     * 生成受控预览 Token。schemaHash 必填，必须与服务端当前草稿哈希一致。
     */
    @PostMapping("/drafts/{pageId}/previews")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PreviewCreateVO> createPreview(
            @PathVariable Long pageId,
            @Valid @RequestBody PreviewCreateDTO dto,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {

        String username = resolveUsername(principal);
        log.info("admin create preview pageId={} operator={}", pageId, username);

        // 门禁：独占编辑锁校验
        editorLockService.validateLock(EditorResourceTypeEnum.PAGE, pageId, lockToken, username);

        // 加载草稿
        PageDraftVO draft = pageDraftService.getDraft(pageId);

        if (draft.getSchemaJson() == null) {
            throw new BusinessException(ErrorCode.PAGE_DRAFT_NOT_FOUND, "草稿内容为空，请先保存内容后再生成预览");
        }

        // schemaHash 必填强校验：客户端传入的哈希必须与服务端草稿哈希一致
        String serverHash = draft.getSchemaHash();
        if (!dto.getSchemaHash().trim().equals(serverHash)) {
            log.warn("preview schemaHash mismatch pageId={} clientHash={} serverHash={}",
                    pageId, dto.getSchemaHash(), serverHash);
            throw new BusinessException(ErrorCode.PAGE_PREVIEW_SCHEMA_HASH_MISMATCH);
        }

        String token = previewTokenService.createToken(
                pageId,
                draft.getSchemaHash(),
                username
        );

        String previewUrl = officialProperties.getPageBuilder().buildPreviewUrl(token);
        PreviewCreateVO vo = new PreviewCreateVO(token, previewUrl, previewTokenService.computeExpiresAt());

        log.info("admin preview created pageId={} token={}", pageId, DataMaskUtils.maskPreviewToken(token));
        return ApiResponse.success(vo);
    }

    /**
     * 主动撤销预览 Token。
     * Token 已过期/已撤销时幂等返回成功；其他异常（锁冲突/归属不符）直接向上抛出。
     */
    @DeleteMapping("/previews/{previewToken}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> revokePreview(
            @PathVariable String previewToken,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {

        String username = resolveUsername(principal);
        log.info("admin revoke preview token={} operator={}",
                DataMaskUtils.maskPreviewToken(previewToken), username);

        PreviewTokenData tokenData;
        try {
            tokenData = previewTokenService.resolveToken(previewToken);
        } catch (BusinessException e) {
            if (e.getErrorCode() == ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED) {
                // Token 已过期或已撤销，幂等成功
                log.info("admin revoke preview: token already expired/revoked token={}",
                        DataMaskUtils.maskPreviewToken(previewToken));
                return ApiResponse.success();
            }
            throw e;
        }

        // Token 有效：仅创建管理员可撤销
        if (tokenData != null && tokenData.getCreatedBy() != null
                && !tokenData.getCreatedBy().equalsIgnoreCase(username)) {
            log.warn("admin revoke preview denied: token={} createdBy={} requestedBy={}",
                    DataMaskUtils.maskPreviewToken(previewToken), tokenData.getCreatedBy(), username);
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "仅创建该预览链接的管理员可以撤销");
        }

        // 校验当前请求者持有该页面编辑锁
        if (tokenData != null && tokenData.getPageId() != null) {
            editorLockService.validateLock(EditorResourceTypeEnum.PAGE, tokenData.getPageId(), lockToken, username);
        }

        previewTokenService.revokeToken(previewToken);
        return ApiResponse.success();
    }

    private String resolveUsername(Object principal) {
        if (principal instanceof AdminUserPrincipal adminUser) {
            return adminUser.getUsername();
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "admin";
    }
}
