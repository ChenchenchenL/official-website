package com.company.officialwebsite.modules.dashboard.vo;

public class AdminDashboardLeadStatsVO {

    private Long totalCount;
    private Long currentMonthNewCount;
    private Long pendingCount;
    private Long handledCount;

    public Long getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(Long totalCount) {
        this.totalCount = totalCount;
    }

    public Long getCurrentMonthNewCount() {
        return currentMonthNewCount;
    }

    public void setCurrentMonthNewCount(Long currentMonthNewCount) {
        this.currentMonthNewCount = currentMonthNewCount;
    }

    public Long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(Long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public Long getHandledCount() {
        return handledCount;
    }

    public void setHandledCount(Long handledCount) {
        this.handledCount = handledCount;
    }
}
