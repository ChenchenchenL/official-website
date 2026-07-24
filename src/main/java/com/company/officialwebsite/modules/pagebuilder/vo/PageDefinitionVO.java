package com.company.officialwebsite.modules.pagebuilder.vo;

import java.time.LocalDateTime;

/**
 * PageDefinitionVO: 页面定义展示对象。
 */
public class PageDefinitionVO {

    private Long id;
    private String pageKey;
    private String name;
    private String routePath;
    private String pageType;
    private String status;
    private Boolean visible;
    private Integer sortOrder;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long sourcePageId;
    private String sourceTemplateCode;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Long getSourcePageId() {
        return sourcePageId;
    }

    public void setSourcePageId(Long sourcePageId) {
        this.sourcePageId = sourcePageId;
    }

    public String getSourceTemplateCode() {
        return sourceTemplateCode;
    }

    public void setSourceTemplateCode(String sourceTemplateCode) {
        this.sourceTemplateCode = sourceTemplateCode;
    }
}
