package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class BusinessTemplateCreateRequestDTO {

    @Pattern(regexp = "^[A-Za-z0-9_]{1,64}$", message = "Template code is invalid")
    private String templateCode;

    @Size(min = 1, max = 128, message = "Template name length must be between 1 and 128")
    private String templateName;

    @Pattern(regexp = "^[A-Za-z0-9_]{1,64}$", message = "Template type is invalid")
    private String templateType;

    @Pattern(regexp = "^[A-Za-z0-9_]{0,64}$", message = "Default business code is invalid")
    private String defaultBusinessCode;

    @Size(max = 128, message = "Default business name length cannot exceed 128")
    private String defaultBusinessName;

    @Positive(message = "Default icon media id must be positive")
    private Long defaultIconMediaId;

    @Pattern(regexp = "^[A-Za-z0-9_]{1,32}$", message = "Default business status is invalid")
    private String defaultBusinessStatus;

    @Size(max = 512, message = "Description length cannot exceed 512")
    private String description;

    private String templateConfig;

    @PositiveOrZero(message = "Sort order cannot be negative")
    private Integer sortOrder;

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
}
