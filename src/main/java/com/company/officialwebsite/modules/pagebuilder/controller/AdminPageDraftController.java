package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDraftSaveDTO;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminPageDraftController：后台页面草稿管理接口，提供草稿查询与保存能力。
 * <p>
 * 所有接口均需持有 {@code ADMINISTRATOR} 角色，业务逻辑委托给 {@link PageDraftService}。
 * 预览 Token 相关操作已迁移至 {@link AdminPreviewController}。
 * </p>
 */
@RestController
@RequestMapping("/admin/api/page-builder/drafts")
public class AdminPageDraftController {

    private static final Logger log = LoggerFactory.getLogger(AdminPageDraftController.class);

    private final PageDraftService pageDraftService;

    public AdminPageDraftController(PageDraftService pageDraftService) {
        this.pageDraftService = pageDraftService;
    }

    /**
     * 查询指定页面的当前草稿。
     *
     * @param pageId 页面定义 ID
     * @return 包含草稿 VO 的统一响应
     */
    @GetMapping("/{pageId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageDraftVO> getDraft(@PathVariable Long pageId) {
        log.info("admin get page draft pageId={}", pageId);
        return ApiResponse.success(pageDraftService.getDraft(pageId));
    }

    /**
     * 保存/更新指定页面的草稿 Schema。
     *
     * @param pageId 页面定义 ID
     * @param dto    包含完整 Schema 和版本号的请求体
     * @return 包含更新后草稿 VO 的统一响应
     */
    @PutMapping("/{pageId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageDraftVO> saveDraft(
            @PathVariable Long pageId,
            @Valid @RequestBody PageDraftSaveDTO dto,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("admin save page draft pageId={} version={} user={}", pageId, dto.getVersion(), username);
        return ApiResponse.success(pageDraftService.saveDraft(pageId, dto, lockToken, username));
    }

    /**
     * 将指定页面的草稿配置重置为当前在线 ACTIVE 发布快照。
     *
     * @param pageId 页面定义 ID
     * @param lockToken 编辑锁凭证
     * @return 包含更新后草稿 VO 的统一响应
     */
    @org.springframework.web.bind.annotation.PostMapping("/{pageId}/reset-to-published")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageDraftVO> resetDraftToPublished(
            @PathVariable Long pageId,
            @org.springframework.web.bind.annotation.RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @org.springframework.security.core.annotation.AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("admin reset page draft to published pageId={} user={}", pageId, username);
        return ApiResponse.success(pageDraftService.resetDraftToPublished(pageId, lockToken, username));
    }

    private String resolveUsername(Object principal) {
        if (principal instanceof com.company.officialwebsite.infrastructure.security.AdminUserPrincipal adminUser) {
            return adminUser.getUsername();
        }
        if (principal instanceof org.springframework.security.core.userdetails.UserDetails userDetails) {
            return userDetails.getUsername();
        }
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getName() != null) {
            return auth.getName();
        }
        return "admin";
    }
}

