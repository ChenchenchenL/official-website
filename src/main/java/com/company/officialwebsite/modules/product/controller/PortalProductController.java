package com.company.officialwebsite.modules.product.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.product.service.ProductEditorService;
import com.company.officialwebsite.modules.product.service.ProductService;
import com.company.officialwebsite.modules.product.vo.PortalProductDetailVO;
import com.company.officialwebsite.modules.product.vo.PortalProductVO;
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
 * PortalProductController：前台产品矩阵公开与受控预览接口。
 */
@RestController
@RequestMapping("/portal/api/products")
public class PortalProductController {

    private final ProductService productService;
    private final ProductEditorService productEditorService;

    public PortalProductController(ProductService productService, ProductEditorService productEditorService) {
        this.productService = productService;
        this.productEditorService = productEditorService;
    }

    /**
     * 获取 Portal 前台可见的产品列表。
     */
    @GetMapping
    public ApiResponse<List<PortalProductVO>> getPortalProducts() {
        return ApiResponse.success(productService.getPortalProducts());
    }

    /**
     * 获取 Portal 前台产品详情（仅允许读取已发布 PUBLISHED 的实体数据）。
     */
    @GetMapping("/{id}")
    public ApiResponse<PortalProductDetailVO> getPortalProductDetail(@PathVariable Long id) {
        return ApiResponse.success(productService.getPortalProductDetail(id));
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
        return ApiResponse.success(productEditorService.renderPreview(id, previewToken, username));
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
