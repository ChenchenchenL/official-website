package com.company.officialwebsite.modules.casecenter.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * CaseVersionVO：标杆案例发布版本 VO。
 */
public class CaseVersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long caseId;
    private Integer versionNo;
    private Object snapshotJson;
    private String snapshotHash;
    private String changeSummary;
    private String publisher;
    private Long rollbackSourceVersionId;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    public CaseVersionVO() {
    }

    public CaseVersionVO(Long id, Long caseId, Integer versionNo, Object snapshotJson, String snapshotHash, String changeSummary, String publisher, Long rollbackSourceVersionId, LocalDateTime publishedAt, LocalDateTime createdAt) {
        this.id = id;
        this.caseId = caseId;
        this.versionNo = versionNo;
        this.snapshotJson = snapshotJson;
        this.snapshotHash = snapshotHash;
        this.changeSummary = changeSummary;
        this.publisher = publisher;
        this.rollbackSourceVersionId = rollbackSourceVersionId;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public Object getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(Object snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public String getSnapshotHash() {
        return snapshotHash;
    }

    public void setSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public Long getRollbackSourceVersionId() {
        return rollbackSourceVersionId;
    }

    public void setRollbackSourceVersionId(Long rollbackSourceVersionId) {
        this.rollbackSourceVersionId = rollbackSourceVersionId;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
