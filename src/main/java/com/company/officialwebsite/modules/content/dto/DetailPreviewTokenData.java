package com.company.officialwebsite.modules.content.dto;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * DetailPreviewTokenData：受控详情（产品、案例、行业方案）预览 Token 关联的数据结构。
 */
public class DetailPreviewTokenData implements Serializable {

    private static final long serialVersionUID = 1L;

    private EditorResourceTypeEnum resourceType;
    private Long resourceId;
    private String draftHash;
    private String createdBy;
    private LocalDateTime createdAt;

    public DetailPreviewTokenData() {
    }

    public DetailPreviewTokenData(EditorResourceTypeEnum resourceType, Long resourceId, String draftHash, String createdBy, LocalDateTime createdAt) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.draftHash = draftHash;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public EditorResourceTypeEnum getResourceType() {
        return resourceType;
    }

    public void setResourceType(EditorResourceTypeEnum resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public String getDraftHash() {
        return draftHash;
    }

    public void setDraftHash(String draftHash) {
        this.draftHash = draftHash;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
