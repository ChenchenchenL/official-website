package com.company.officialwebsite.modules.dashboard.service;

import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardBusinessStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardContentStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardLeadStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardMediaStatsVO;
import com.company.officialwebsite.modules.dashboard.vo.AdminDashboardRiskAlertsVO;

public interface AdminDashboardService {

    AdminDashboardContentStatsVO getContentStats();

    AdminDashboardLeadStatsVO getLeadStats();

    AdminDashboardMediaStatsVO getMediaStats();

    AdminDashboardBusinessStatsVO getBusinessStats();

    AdminDashboardRiskAlertsVO getRiskAlerts();
}
