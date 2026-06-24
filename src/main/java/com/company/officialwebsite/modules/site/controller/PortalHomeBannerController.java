package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.HomeBannerService;
import com.company.officialwebsite.modules.site.vo.PortalHomeBannerVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalHomeBannerController：提供前台公开的首页首屏主视觉查询接口。
 */
@RestController
@RequestMapping("/portal/api/site/home-banner")
public class PortalHomeBannerController {

    private final HomeBannerService homeBannerService;

    public PortalHomeBannerController(HomeBannerService homeBannerService) {
        this.homeBannerService = homeBannerService;
    }

    /**
     * 返回前台可直接渲染的首页首屏主视觉配置。
     */
    @GetMapping
    public ApiResponse<PortalHomeBannerVO> getBanner() {
        return ApiResponse.success(homeBannerService.getPortalBanner());
    }
}
