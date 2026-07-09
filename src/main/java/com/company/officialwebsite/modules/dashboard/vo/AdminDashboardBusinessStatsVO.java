package com.company.officialwebsite.modules.dashboard.vo;

import java.util.List;

public class AdminDashboardBusinessStatsVO {

    private List<BusinessModuleStatsVO> modules;

    public List<BusinessModuleStatsVO> getModules() {
        return modules;
    }

    public void setModules(List<BusinessModuleStatsVO> modules) {
        this.modules = modules;
    }

    public static class BusinessModuleStatsVO {

        private String businessCode;
        private String businessName;
        private String businessStatus;
        private Long pageCount;
        private Long pageBlockCount;
        private Long contentCount;

        public String getBusinessCode() {
            return businessCode;
        }

        public void setBusinessCode(String businessCode) {
            this.businessCode = businessCode;
        }

        public String getBusinessName() {
            return businessName;
        }

        public void setBusinessName(String businessName) {
            this.businessName = businessName;
        }

        public String getBusinessStatus() {
            return businessStatus;
        }

        public void setBusinessStatus(String businessStatus) {
            this.businessStatus = businessStatus;
        }

        public Long getPageCount() {
            return pageCount;
        }

        public void setPageCount(Long pageCount) {
            this.pageCount = pageCount;
        }

        public Long getPageBlockCount() {
            return pageBlockCount;
        }

        public void setPageBlockCount(Long pageBlockCount) {
            this.pageBlockCount = pageBlockCount;
        }

        public Long getContentCount() {
            return contentCount;
        }

        public void setContentCount(Long contentCount) {
            this.contentCount = contentCount;
        }
    }
}
