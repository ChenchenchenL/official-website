package com.company.officialwebsite.modules.dashboard.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.dashboard.service.AdminDashboardService;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardBusinessStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardContentStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardLeadStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardMediaStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardRiskAlertsVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/dashboard")
public class AdminDashboardController {

    private static final Logger log = LoggerFactory.getLogger(AdminDashboardController.class);

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/content-stats")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminDashboardContentStatsVO> getContentStats() {
        log.info("get admin dashboard content stats request");
        return ApiResponse.success(adminDashboardService.getContentStats());
    }

    @GetMapping("/lead-stats")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminDashboardLeadStatsVO> getLeadStats() {
        log.info("get admin dashboard lead stats request");
        return ApiResponse.success(adminDashboardService.getLeadStats());
    }

    @GetMapping("/media-stats")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminDashboardMediaStatsVO> getMediaStats() {
        log.info("get admin dashboard media stats request");
        return ApiResponse.success(adminDashboardService.getMediaStats());
    }

    @GetMapping("/business-stats")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminDashboardBusinessStatsVO> getBusinessStats() {
        log.info("get admin dashboard business stats request");
        return ApiResponse.success(adminDashboardService.getBusinessStats());
    }

    @GetMapping("/risk-alerts")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminDashboardRiskAlertsVO> getRiskAlerts() {
        log.info("get admin dashboard risk alerts request");
        return ApiResponse.success(adminDashboardService.getRiskAlerts());
    }
}
