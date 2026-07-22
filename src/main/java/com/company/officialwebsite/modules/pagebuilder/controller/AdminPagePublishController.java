package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.pagebuilder.dto.PagePublishDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageRollbackDTO;
import com.company.officialwebsite.modules.pagebuilder.service.PagePublishService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageVersionVO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AdminPagePublishController: 后台页面发布、回滚与版本历史管理接口。
 */
@RestController
@RequestMapping("/admin/api/page-builder/pages")
@Validated
public class AdminPagePublishController {

    private static final Logger log = LoggerFactory.getLogger(AdminPagePublishController.class);

    private final PagePublishService pagePublishService;

    public AdminPagePublishController(PagePublishService pagePublishService) {
        this.pagePublishService = pagePublishService;
    }

    /**
     * 将指定页面草稿发布上线。
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageVersionVO> publishPage(
            @PathVariable Long id,
            @Valid @RequestBody PagePublishDTO dto,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("Admin request: publish page id={} user={}", id, username);
        return ApiResponse.success(pagePublishService.publishPage(id, dto, lockToken, username));
    }

    /**
     * 将指定页面回滚到历史发布版本。
     */
    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageVersionVO> rollbackPage(
            @PathVariable Long id,
            @Valid @RequestBody PageRollbackDTO dto,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("Admin request: rollback page id={} to versionId={} user={}", id, dto.getVersionId(), username);
        return ApiResponse.success(pagePublishService.rollbackPage(id, dto, lockToken, username));
    }

    /**
     * 分页查询指定页面的历史发布版本摘要列表（默认排除全量 schemaJson 字段）。
     */
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<PageVersionVO>> getVersions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Admin request: get versions list for page id={} pageNo={} pageSize={}", id, pageNo, pageSize);
        return ApiResponse.success(pagePublishService.listVersions(id, pageNo, pageSize));
    }

    /**
     * 查询指定页面单个历史版本的配置全量详情（包含完整 schemaJson）。
     */
    @GetMapping("/{id}/versions/{versionId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageVersionVO> getVersionDetail(
            @PathVariable Long id,
            @PathVariable Long versionId) {
        log.info("Admin request: get version detail for page id={} versionId={}", id, versionId);
        return ApiResponse.success(pagePublishService.getVersionDetail(id, versionId));
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
