package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.HomeBannerUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.HomeBannerService;
import com.company.officialwebsite.modules.site.vo.AdminHomeBannerVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminHomeBannerController：提供后台首页首屏主视觉配置管理接口。
 */
@RestController
@RequestMapping("/admin/api/site/home-banner")
public class AdminHomeBannerController {

    private final HomeBannerService homeBannerService;

    public AdminHomeBannerController(HomeBannerService homeBannerService) {
        this.homeBannerService = homeBannerService;
    }

    /**
     * 返回后台当前可编辑的首页首屏主视觉配置。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminHomeBannerVO> getBanner() {
        return ApiResponse.success(homeBannerService.getAdminBanner());
    }

    /**
     * 更新首页首屏主视觉文案、背景图和双 CTA 配置。
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminHomeBannerVO> updateBanner(@Valid @RequestBody HomeBannerUpdateRequestDTO requestDTO) {
        return ApiResponse.success(homeBannerService.updateBanner(requestDTO));
    }
}
