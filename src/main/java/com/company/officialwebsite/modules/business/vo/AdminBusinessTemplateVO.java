package com.company.officialwebsite.modules.business.vo;

import java.time.LocalDateTime;

public class AdminBusinessTemplateVO {

    private Long id;
    private String templateCode;
    private String templateName;
    private String templateType;
    private String defaultBusinessCode;
    private String defaultBusinessName;
    private Long defaultIconMediaId;
    private String defaultIconUrl;
    private String defaultBusinessStatus;
    private String description;
    private String templateConfig;
    private Integer sortOrder;
    private Integer version;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getTemplateType() {
        return templateType;
    }

    public void setTemplateType(String templateType) {
        this.templateType = templateType;
    }

    public String getDefaultBusinessCode() {
        return defaultBusinessCode;
    }

    public void setDefaultBusinessCode(String defaultBusinessCode) {
        this.defaultBusinessCode = defaultBusinessCode;
    }

    public String getDefaultBusinessName() {
        return defaultBusinessName;
    }

    public void setDefaultBusinessName(String defaultBusinessName) {
        this.defaultBusinessName = defaultBusinessName;
    }

    public Long getDefaultIconMediaId() {
        return defaultIconMediaId;
    }

    public void setDefaultIconMediaId(Long defaultIconMediaId) {
        this.defaultIconMediaId = defaultIconMediaId;
    }

    public String getDefaultIconUrl() {
        return defaultIconUrl;
    }

    public void setDefaultIconUrl(String defaultIconUrl) {
        this.defaultIconUrl = defaultIconUrl;
    }

    public String getDefaultBusinessStatus() {
        return defaultBusinessStatus;
    }

    public void setDefaultBusinessStatus(String defaultBusinessStatus) {
        this.defaultBusinessStatus = defaultBusinessStatus;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateConfig() {
        return templateConfig;
    }

    public void setTemplateConfig(String templateConfig) {
        this.templateConfig = templateConfig;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
