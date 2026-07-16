package com.company.officialwebsite.modules.product.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.product.service.IndustrySolutionEditorService;
import com.company.officialwebsite.modules.product.service.IndustrySolutionService;
import com.company.officialwebsite.modules.product.vo.PortalIndustrySolutionDetailVO;
import com.company.officialwebsite.modules.product.vo.PortalIndustrySolutionVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PortalIndustrySolutionController：前台行业解决方案公开与受控预览接口。
 */
@RestController
@RequestMapping("/portal/api/industry-solutions")
public class PortalIndustrySolutionController {

    private final IndustrySolutionService industrySolutionService;
    private final IndustrySolutionEditorService solutionEditorService;

    public PortalIndustrySolutionController(
            IndustrySolutionService industrySolutionService,
            IndustrySolutionEditorService solutionEditorService) {
        this.industrySolutionService = industrySolutionService;
        this.solutionEditorService = solutionEditorService;
    }

    /**
     * 获取 Portal 前台可见的行业解决方案列表。
     */
    @GetMapping
    public ApiResponse<List<PortalIndustrySolutionVO>> getPortalIndustrySolutions() {
        return ApiResponse.success(industrySolutionService.getPortalIndustrySolutions());
    }

    /**
     * 获取 Portal 前台行业解决方案详情（仅允许读取已发布 PUBLISHED 且 Visible 的正式快照）。
     */
    @GetMapping("/{id}")
    public ApiResponse<PortalIndustrySolutionDetailVO> getPortalIndustrySolutionDetail(@PathVariable Long id) {
        return ApiResponse.success(solutionEditorService.getPortalSolutionDetail(id));
    }

    /**
     * Portal 侧受控草稿预览渲染（需管理员 Session 且匹配创建者）。
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
        return ApiResponse.success(solutionEditorService.renderPreview(id, previewToken, username));
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
