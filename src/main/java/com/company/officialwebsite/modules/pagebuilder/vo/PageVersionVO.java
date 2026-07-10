package com.company.officialwebsite.modules.pagebuilder.vo;

import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;

import java.time.LocalDateTime;

/**
 * PageVersionVO: 页面历史版本数据展示模型。
 */
public class PageVersionVO {

    private Long id;
    private Long pageId;
    private Integer versionNo;
    private String sourceType;
    private PageSchemaModel schemaJson;
    private String schemaHash;
    private String changeSummary;
    private Integer version;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public PageSchemaModel getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(PageSchemaModel schemaJson) {
        this.schemaJson = schemaJson;
    }

    public String getSchemaHash() {
        return schemaHash;
    }

    public void setSchemaHash(String schemaHash) {
        this.schemaHash = schemaHash;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
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
}
