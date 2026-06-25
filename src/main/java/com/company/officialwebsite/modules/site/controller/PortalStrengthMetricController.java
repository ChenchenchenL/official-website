package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.StrengthMetricService;
import com.company.officialwebsite.modules.site.vo.PortalStrengthMetricVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalStrengthMetricController：提供前台公开企业实力核心指标接口，无需登录。
 * 仅返回 visible=1 且未删除的指标，不暴露审计字段、版本号及删除标记。
 * 支持 Redis 缓存，缓存失效由后台写操作事务提交后触发。
 */
@RestController
@RequestMapping("/portal/api/site/strength-metrics")
public class PortalStrengthMetricController {

    private final StrengthMetricService strengthMetricService;

    public PortalStrengthMetricController(StrengthMetricService strengthMetricService) {
        this.strengthMetricService = strengthMetricService;
    }

    @GetMapping
    public ApiResponse<List<PortalStrengthMetricVO>> getPortalMetrics() {
        return ApiResponse.success(strengthMetricService.getPortalMetrics());
    }
}
