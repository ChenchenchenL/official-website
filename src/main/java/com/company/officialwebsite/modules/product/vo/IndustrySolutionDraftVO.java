package com.company.officialwebsite.modules.product.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * IndustrySolutionDraftVO：行业解决方案详情草稿 VO。
 */
public class IndustrySolutionDraftVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long solutionId;
    private Object draftJson;
    private String draftHash;
    private String editorSessionRemark;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public IndustrySolutionDraftVO() {
    }

    public IndustrySolutionDraftVO(Long id, Long solutionId, Object draftJson, String draftHash, String editorSessionRemark, Integer version, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.solutionId = solutionId;
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

    public Long getSolutionId() {
        return solutionId;
    }

    public void setSolutionId(Long solutionId) {
        this.solutionId = solutionId;
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
