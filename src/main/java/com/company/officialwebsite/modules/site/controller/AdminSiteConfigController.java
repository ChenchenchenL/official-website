package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.SiteConfigUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.SiteConfigService;
import com.company.officialwebsite.modules.site.vo.AdminSiteConfigVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminSiteConfigController：提供后台站点基础配置管理接口。
 */
@RestController
@RequestMapping("/admin/api/site/config")
public class AdminSiteConfigController {

    private final SiteConfigService siteConfigService;

    public AdminSiteConfigController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    /**
     * 返回后台当前可编辑的站点基础配置。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminSiteConfigVO> getConfig() {
        return ApiResponse.success(siteConfigService.getAdminConfig());
    }

    /**
     * 更新网站标题、SEO 信息、品牌文案和亮暗双 Logo。
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminSiteConfigVO> updateConfig(@Valid @RequestBody SiteConfigUpdateRequestDTO requestDTO) {
        return ApiResponse.success(siteConfigService.updateConfig(requestDTO));
    }
}
