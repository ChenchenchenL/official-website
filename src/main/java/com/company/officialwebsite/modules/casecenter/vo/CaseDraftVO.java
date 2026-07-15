package com.company.officialwebsite.modules.casecenter.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CaseDraftVO：标杆案例详情草稿 VO。
 */
public class CaseDraftVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long caseId;
    private Object draftJson;
    private String draftHash;
    private String editorSessionRemark;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CaseDraftVO() {
    }

    public CaseDraftVO(Long id, Long caseId, Object draftJson, String draftHash, String editorSessionRemark, Integer version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.caseId = caseId;
        this.draftJson = draftJson;
        this.draftHash = draftHash;
        this.editorSessionRemark = editorSessionRemark;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Object getDraftJson() {
        return draftJson;
    }

    public void setDraftJson(Object draftJson) {
        this.draftJson = draftJson;
    }

    public String getDraftHash() {
        return draftHash;
    }

    public void setDraftHash(String draftHash) {
        this.draftHash = draftHash;
    }

    public String getEditorSessionRemark() {
        return editorSessionRemark;
    }

    public void setEditorSessionRemark(String editorSessionRemark) {
        this.editorSessionRemark = editorSessionRemark;
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
}
