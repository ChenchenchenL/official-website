package com.company.officialwebsite.modules.casecenter.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.casecenter.service.CaseEditorService;
import com.company.officialwebsite.modules.casecenter.service.CaseService;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseDetailVO;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO;
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
 * PortalCaseController：前台标杆案例公开与受控预览接口。
 */
@RestController
@RequestMapping("/portal/api/cases")
public class PortalCaseController {

    private final CaseService caseService;
    private final CaseEditorService caseEditorService;

    public PortalCaseController(CaseService caseService, CaseEditorService caseEditorService) {
        this.caseService = caseService;
        this.caseEditorService = caseEditorService;
    }

    /**
     * 获取 Portal 前台可见的案例列表。
     */
    @GetMapping
    public ApiResponse<List<PortalCaseVO>> getPortalCases() {
        return ApiResponse.success(caseService.getPortalCases());
    }

    /**
     * 获取 Portal 前台案例详情（仅允许读取已发布 PUBLISHED 的实体数据）。
     */
    @GetMapping("/{id}")
    public ApiResponse<PortalCaseDetailVO> getPortalCaseDetail(@PathVariable Long id) {
        return ApiResponse.success(caseService.getPortalCaseDetail(id));
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
        return ApiResponse.success(caseEditorService.renderPreview(id, previewToken, username));
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
