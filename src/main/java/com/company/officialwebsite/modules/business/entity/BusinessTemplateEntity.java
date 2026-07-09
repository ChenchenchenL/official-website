package com.company.officialwebsite.modules.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

@TableName("business_template")
public class BusinessTemplateEntity extends BaseEntity {

    private String templateCode;
    private String templateName;
    private String templateType;
    private String defaultBusinessCode;
    private String defaultBusinessName;
    private Long defaultIconMediaId;
    private String defaultBusinessStatus;
    private String description;
    private String templateConfig;
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
