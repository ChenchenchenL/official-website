package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.SiteConfigService;
import com.company.officialwebsite.modules.site.vo.PortalSiteConfigVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalSiteConfigController：提供前台公开的站点基础配置查询接口。
 */
@RestController
@RequestMapping("/portal/api/site/config")
public class PortalSiteConfigController {

    private final SiteConfigService siteConfigService;

    public PortalSiteConfigController(SiteConfigService siteConfigService) {
        this.siteConfigService = siteConfigService;
    }

    /**
     * 返回前台可直接用于 SEO 和全局品牌展示的站点配置。
     */
    @GetMapping
    public ApiResponse<PortalSiteConfigVO> getConfig() {
        return ApiResponse.success(siteConfigService.getPortalConfig());
    }
}
