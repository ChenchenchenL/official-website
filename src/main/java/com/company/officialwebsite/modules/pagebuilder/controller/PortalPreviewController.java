package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.application.portal.PageRenderApplicationService;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.utils.DataMaskUtils;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.pagebuilder.model.PreviewTokenData;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftService;
import com.company.officialwebsite.modules.pagebuilder.service.PreviewTokenService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageVO;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalPreviewController：Portal 侧受控草稿预览渲染接口。
 * <p>
 * 安全约束：
 * 1. 必须具备有效管理员身份；
 * 2. 只有创建预览 Token 的管理员可以查看该预览；
 * 3. HTTP 响应固定包含 Cache-Control: no-store 与 Referrer-Policy: no-referrer。
 * </p>
 */
@RestController
@RequestMapping("/portal/api/page-builder/previews")
public class PortalPreviewController {

    private static final Logger log = LoggerFactory.getLogger(PortalPreviewController.class);

    private final PreviewTokenService previewTokenService;
    private final PageRenderApplicationService pageRenderApplicationService;
    private final PageDraftService pageDraftService;

    public PortalPreviewController(
            PreviewTokenService previewTokenService,
            PageRenderApplicationService pageRenderApplicationService,
            PageDraftService pageDraftService) {
        this.previewTokenService = previewTokenService;
        this.pageRenderApplicationService = pageRenderApplicationService;
        this.pageDraftService = pageDraftService;
    }

    /**
     * 通过受控预览 Token 渲染草稿页面（需管理员已登录且匹配 Token 创建者）。
     */
    @GetMapping("/{previewToken}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PortalPageVO> getPreview(
            @PathVariable String previewToken,
            HttpServletResponse response,
            @AuthenticationPrincipal Object principal) {

        // 强制禁止任何缓存层与 Referrer 泄漏
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");

        String username = resolveUsername(principal);
        log.info("portal preview request token={} user={}", DataMaskUtils.maskPreviewToken(previewToken), username);

        // 解析 Token → 获取 pageId（Token 不存在/过期/撤销时抛 PAGE_PREVIEW_TOKEN_EXPIRED → HTTP 401）
        PreviewTokenData tokenData = previewTokenService.resolveToken(previewToken);

        // 检查当前登录管理员是否为创建预览的管理员
        if (tokenData.getCreatedBy() != null && !tokenData.getCreatedBy().equalsIgnoreCase(username)) {
            log.warn("portal preview access denied token={} createdBy={} user={}",
                    DataMaskUtils.maskPreviewToken(previewToken), tokenData.getCreatedBy(), username);
            throw new BusinessException(ErrorCode.AUTH_FORBIDDEN, "仅限创建该预览链接的管理员访问");
        }

        Long pageId = tokenData.getPageId();

        // 比对 Token 绑定的草稿哈希与当前草稿哈希，防止旧预览链接展示新草稿内容
        if (StringUtils.hasText(tokenData.getSchemaHash())) {
            try {
                PageDraftVO currentDraft = pageDraftService.getDraft(pageId);
                if (currentDraft.getSchemaHash() != null
                        && !tokenData.getSchemaHash().equals(currentDraft.getSchemaHash())) {
                    log.warn("portal preview hash mismatch: token hash={} current draft hash={} pageId={}",
                            tokenData.getSchemaHash(), currentDraft.getSchemaHash(), pageId);
                    throw new BusinessException(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED,
                            "预览链接已失效，草稿已更新，请重新生成预览");
                }
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.warn("portal preview: failed to load current draft for hash check pageId={}", pageId, e);
                throw new BusinessException(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED);
            }
        }

        // 使用无缓存预览渲染链路：绑定装配 + 可见性过滤 + binding 元数据清除
        PortalPageVO vo = pageRenderApplicationService.renderDraftForPreview(pageId);

        log.info("portal preview rendered pageId={} token={}", pageId, DataMaskUtils.maskPreviewToken(previewToken));
        return ApiResponse.success(vo);
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
