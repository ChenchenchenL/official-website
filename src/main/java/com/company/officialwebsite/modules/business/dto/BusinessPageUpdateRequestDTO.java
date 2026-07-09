package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class BusinessPageUpdateRequestDTO {

    @NotNull(message = "Version cannot be null")
    @PositiveOrZero(message = "Version cannot be negative")
    private Integer version;

    @NotNull(message = "Business id cannot be null")
    @Positive(message = "Business id must be positive")
    private Long businessId;

    @Positive(message = "Template id must be positive")
    private Long templateId;

    @Pattern(regexp = "^[A-Za-z0-9_]{1,64}$", message = "Page code is invalid")
    private String pageCode;

    @Size(min = 1, max = 128, message = "Page name length must be between 1 and 128")
    private String pageName;

    @Size(min = 1, max = 256, message = "Route path length must be between 1 and 256")
    private String routePath;

    @Pattern(regexp = "^[A-Za-z0-9_]{1,32}$", message = "Page status is invalid")
    private String pageStatus;

    @Size(max = 4000, message = "Page config length cannot exceed 4000")
    private String pageConfig;

    private Boolean visible;

    @PositiveOrZero(message = "Sort order cannot be negative")
    private Integer sortOrder;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getBusinessId() {
        return businessId;
    }

    public void setBusinessId(Long businessId) {
        this.businessId = businessId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getPageStatus() {
        return pageStatus;
    }

    public void setPageStatus(String pageStatus) {
        this.pageStatus = pageStatus;
    }

    public String getPageConfig() {
        return pageConfig;
    }

    public void setPageConfig(String pageConfig) {
        this.pageConfig = pageConfig;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
