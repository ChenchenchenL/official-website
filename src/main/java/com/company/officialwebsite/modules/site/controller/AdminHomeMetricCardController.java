package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardOrderRequestDTO;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardUpdateRequestDTO;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardVisibilityUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.HomeMetricCardService;
import com.company.officialwebsite.modules.site.vo.AdminHomeMetricCardVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminHomeMetricCardController：提供后台首页核心数据指标卡片管理接口。
 */
@RestController
@RequestMapping("/admin/api/site/home-metrics")
public class AdminHomeMetricCardController {

    private final HomeMetricCardService homeMetricCardService;

    public AdminHomeMetricCardController(HomeMetricCardService homeMetricCardService) {
        this.homeMetricCardService = homeMetricCardService;
    }

    /**
     * 返回后台当前可编辑的首页核心数据指标卡片列表。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHomeMetricCardVO>> getMetricCards() {
        return ApiResponse.success(homeMetricCardService.getAdminMetricCards());
    }

    /**
     * 新增一张首页核心数据指标卡片。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHomeMetricCardVO>> createMetricCard(
            @Valid @RequestBody HomeMetricCardCreateRequestDTO requestDTO) {
        return ApiResponse.success(homeMetricCardService.createMetricCard(requestDTO));
    }

    /**
     * 更新指定首页核心数据指标卡片内容。
     */
    @PutMapping("/{metricId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHomeMetricCardVO>> updateMetricCard(
            @PathVariable Long metricId,
            @Valid @RequestBody HomeMetricCardUpdateRequestDTO requestDTO) {
        return ApiResponse.success(homeMetricCardService.updateMetricCard(metricId, requestDTO));
    }

    /**
     * 单独更新指定首页核心数据指标卡片显示状态。
     */
    @PutMapping("/{metricId}/visibility")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHomeMetricCardVO>> updateVisibility(
            @PathVariable Long metricId,
            @Valid @RequestBody HomeMetricCardVisibilityUpdateRequestDTO requestDTO) {
        return ApiResponse.success(homeMetricCardService.updateVisibility(metricId, requestDTO));
    }

    /**
     * 删除指定首页核心数据指标卡片。
     */
    @DeleteMapping("/{metricId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHomeMetricCardVO>> deleteMetricCard(@PathVariable Long metricId) {
        return ApiResponse.success(homeMetricCardService.deleteMetricCard(metricId));
    }

    /**
     * 提交首页核心数据指标卡片完整排序。
     */
    @PutMapping("/order")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHomeMetricCardVO>> reorderMetricCards(
            @Valid @RequestBody HomeMetricCardOrderRequestDTO requestDTO) {
        return ApiResponse.success(homeMetricCardService.reorderMetricCards(requestDTO));
    }
}
