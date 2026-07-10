package com.company.officialwebsite.modules.dashboard.vo;

public class AdminDashboardRiskAlertsVO {

    private Long unpublishedContentCount;
    private Long invalidContentCount;
    private Long referencedContentCount;
    private Long pendingLeadCount;

    public Long getUnpublishedContentCount() {
        return unpublishedContentCount;
    }

    public void setUnpublishedContentCount(Long unpublishedContentCount) {
        this.unpublishedContentCount = unpublishedContentCount;
    }

    public Long getInvalidContentCount() {
        return invalidContentCount;
    }

    public void setInvalidContentCount(Long invalidContentCount) {
        this.invalidContentCount = invalidContentCount;
    }

    public Long getReferencedContentCount() {
        return referencedContentCount;
    }

    public void setReferencedContentCount(Long referencedContentCount) {
        this.referencedContentCount = referencedContentCount;
    }

    public Long getPendingLeadCount() {
        return pendingLeadCount;
    }

    public void setPendingLeadCount(Long pendingLeadCount) {
        this.pendingLeadCount = pendingLeadCount;
    }
}
