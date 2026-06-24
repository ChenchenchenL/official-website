package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.HomeMetricCardService;
import com.company.officialwebsite.modules.site.vo.PortalHomeMetricCardVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalHomeMetricCardController：提供前台公开首页核心数据指标卡片接口。
 */
@RestController
@RequestMapping("/portal/api/site/home-metrics")
public class PortalHomeMetricCardController {

    private final HomeMetricCardService homeMetricCardService;

    public PortalHomeMetricCardController(HomeMetricCardService homeMetricCardService) {
        this.homeMetricCardService = homeMetricCardService;
    }

    /**
     * 返回官网前台公开可见的首页核心数据指标卡片列表。
     */
    @GetMapping
    public ApiResponse<List<PortalHomeMetricCardVO>> getMetricCards() {
        return ApiResponse.success(homeMetricCardService.getPortalMetricCards());
    }
}
