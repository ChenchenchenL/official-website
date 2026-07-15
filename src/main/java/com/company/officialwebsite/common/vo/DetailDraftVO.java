package com.company.officialwebsite.common.vo;

import java.time.LocalDateTime;

/**
 * DetailDraftVO：产品/案例/行业方案详情草稿统一响应 VO。
 */
public class DetailDraftVO {

    private Long id;
    private Long resourceId;
    private Object draft;
    private String draftHash;
    private Integer version;
    private LocalDateTime updatedAt;

    public DetailDraftVO() {
    }

    public DetailDraftVO(Long id, Long resourceId, Object draft, String draftHash, Integer version, LocalDateTime updatedAt) {
        this.id = id;
        this.resourceId = resourceId;
        this.draft = draft;
        this.draftHash = draftHash;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Object getDraft() {
        return draft;
    }

    public void setDraft(Object draft) {
        this.draft = draft;
    }

    public String getDraftHash() {
        return draftHash;
    }

    public void setDraftHash(String draftHash) {
        this.draftHash = draftHash;
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
