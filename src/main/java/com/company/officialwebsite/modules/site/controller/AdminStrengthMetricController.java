package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.StrengthMetricBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.StrengthMetricCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.StrengthMetricUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.StrengthMetricService;
import com.company.officialwebsite.modules.site.vo.AdminStrengthMetricVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminStrengthMetricController：提供后台企业实力核心指标大盘管理接口。
 *
 * <p>所有接口均需登录且角色为 ADMINISTRATOR。写接口须携带有效 CSRF Token。
 * Controller 仅负责参数接收与校验，业务逻辑、事务、审计、缓存均在 Service 层完成。
 */
@Validated
@RestController
@RequestMapping("/admin/api/site/strength-metrics")
public class AdminStrengthMetricController {

    private final StrengthMetricService strengthMetricService;

    public AdminStrengthMetricController(StrengthMetricService strengthMetricService) {
        this.strengthMetricService = strengthMetricService;
    }

    /**
     * 查询后台全量活跃指标列表（按 sort_order 升序）。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminStrengthMetricVO>> getMetricList() {
        return ApiResponse.success(strengthMetricService.getAdminMetrics());
    }

    /**
     * 新增企业实力核心指标，返回最新全量列表。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminStrengthMetricVO>> createMetric(
            @Valid @RequestBody StrengthMetricCreateRequestDTO requestDTO) {
        return ApiResponse.success(strengthMetricService.createMetric(requestDTO));
    }

    /**
     * 编辑企业实力核心指标，请求体中须携带 version 乐观锁版本号。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminStrengthMetricVO>> updateMetric(
            @PathVariable Long id,
            @Valid @RequestBody StrengthMetricUpdateRequestDTO requestDTO) {
        return ApiResponse.success(strengthMetricService.updateMetric(id, requestDTO));
    }

    /**
     * 逻辑删除企业实力核心指标，通过 version 参数进行乐观锁并发保护。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminStrengthMetricVO>> deleteMetric(
            @PathVariable Long id,
            @RequestParam @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        return ApiResponse.success(strengthMetricService.deleteMetric(id, version));
    }

    /**
     * 批量拖拽排序，传入有序指标 ID 列表（顺序即为期望的前台展示顺序）。
     * 列表必须完整覆盖全部活跃指标，不允许遗漏或重复。
     */
    @PutMapping("/batch-sort")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminStrengthMetricVO>> batchSortMetrics(
            @Valid @RequestBody StrengthMetricBatchSortRequestDTO requestDTO) {
        return ApiResponse.success(strengthMetricService.reorderMetrics(requestDTO));
    }
}
