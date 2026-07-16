package com.company.officialwebsite.modules.product.controller;

import com.company.officialwebsite.common.dto.DetailDraftSaveDTO;
import com.company.officialwebsite.common.dto.DetailOfflineDTO;
import com.company.officialwebsite.common.dto.DetailPreviewCreateDTO;
import com.company.officialwebsite.common.dto.DetailPublishDTO;
import com.company.officialwebsite.common.dto.DetailRollbackDTO;
import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.product.service.IndustrySolutionEditorService;
import com.company.officialwebsite.modules.product.vo.IndustrySolutionDraftVO;
import com.company.officialwebsite.modules.product.vo.IndustrySolutionVersionVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AdminIndustrySolutionEditorController：后台行业解决方案详情可视化编辑与全生命周期控制管理 API。
 */
@RestController
@RequestMapping("/admin/api/industry-solutions")
@Validated
public class AdminIndustrySolutionEditorController {

    private static final Logger log = LoggerFactory.getLogger(AdminIndustrySolutionEditorController.class);

    private final IndustrySolutionEditorService solutionEditorService;

    public AdminIndustrySolutionEditorController(IndustrySolutionEditorService solutionEditorService) {
        this.solutionEditorService = solutionEditorService;
    }

    /**
     * 1. 创建新行业方案草稿外壳。
     */
    @PostMapping("/drafts")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<IndustrySolutionDraftVO> createDraft(
            @AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("admin create industry solution draft shell operator={}", username);
        return ApiResponse.success(solutionEditorService.createDraftShell(username));
    }

    /**
     * 2. 查询指定行业方案的当前草稿。
     */
    @GetMapping("/{id}/draft")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<IndustrySolutionDraftVO> getDraft(@PathVariable Long id) {
        log.info("admin get industry solution draft id={}", id);
        return ApiResponse.success(solutionEditorService.getDraft(id));
    }

    /**
     * 3. 保存/更新指定行业方案的草稿。
     */
    @PutMapping("/{id}/draft")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<IndustrySolutionDraftVO> saveDraft(
            @PathVariable Long id,
            @Valid @RequestBody DetailDraftSaveDTO saveDTO,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("admin save industry solution draft id={} version={} operator={}", id, saveDTO.getVersion(), username);
        return ApiResponse.success(solutionEditorService.saveDraft(id, saveDTO, lockToken, username));
    }

    /**
     * 4. 创建受控行业方案预览 Token。
     */
    @PostMapping("/{id}/draft/previews")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<String> createPreviewToken(
            @PathVariable Long id,
            @Valid @RequestBody DetailPreviewCreateDTO previewCreateDTO,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("admin create industry solution preview token id={} operator={}", id, username);
        return ApiResponse.success(solutionEditorService.createPreviewToken(
                id, previewCreateDTO.getDraftHash(), lockToken, username));
    }

    /**
     * 5. 渲染受控行业方案预览草稿。
     */
    @GetMapping("/{id}/previews/{previewToken}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Object> getPreview(
            @PathVariable Long id,
            @PathVariable String previewToken,
            HttpServletResponse response,
            @AuthenticationPrincipal Object principal) {
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Referrer-Policy", "no-referrer");

        String username = resolveUsername(principal);
        log.info("admin get industry solution preview id={} operator={}", id, username);
        return ApiResponse.success(solutionEditorService.renderPreview(id, previewToken, username));
    }

    /**
     * 6. 撤销受控行业方案预览 Token。
     */
    @DeleteMapping("/{id}/previews/{previewToken}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> revokePreviewToken(
            @PathVariable Long id,
            @PathVariable String previewToken,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("admin revoke industry solution preview token id={} operator={}", id, username);
        solutionEditorService.revokePreviewToken(id, previewToken, lockToken, username);
        return ApiResponse.success();
    }

    /**
     * 7. 发布行业方案草稿上线。
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<IndustrySolutionVersionVO> publish(
            @PathVariable Long id,
            @Valid @RequestBody DetailPublishDTO publishDTO,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("admin publish industry solution id={} version={} operator={}", id, publishDTO.getVersion(), username);
        return ApiResponse.success(solutionEditorService.publish(id, publishDTO, lockToken, username));
    }

    /**
     * 8. 查询历史发布版本列表。
     */
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<IndustrySolutionVersionVO>> listVersions(@PathVariable Long id) {
        log.info("admin list industry solution versions id={}", id);
        return ApiResponse.success(solutionEditorService.listVersions(id));
    }

    /**
     * 9. 回滚行业方案至指定历史版本。
     */
    @PostMapping("/{id}/rollback/{versionId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<IndustrySolutionVersionVO> rollback(
            @PathVariable Long id,
            @PathVariable Long versionId,
            @Valid @RequestBody DetailRollbackDTO rollbackDTO,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("admin rollback industry solution id={} to versionId={} operator={}", id, versionId, username);
        return ApiResponse.success(solutionEditorService.rollback(id, versionId, rollbackDTO, lockToken, username));
    }

    /**
     * 10. 下线指定行业方案。
     */
    @PostMapping("/{id}/offline")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> offline(
            @PathVariable Long id,
            @Valid @RequestBody DetailOfflineDTO offlineDTO,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            @AuthenticationPrincipal Object principal) {
        String username = resolveUsername(principal);
        log.info("admin offline industry solution id={} operator={}", id, username);
        solutionEditorService.offline(id, offlineDTO, lockToken, username);
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
