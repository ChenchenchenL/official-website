package com.company.officialwebsite.modules.casecenter.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * CaseVersionEntity：标杆案例发布版本历史实体类。
 */
@TableName("cms_case_version")
public class CaseVersionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("case_id")
    private Long caseId;

    @TableField("version_no")
    private Integer versionNo;

    @TableField("snapshot_json")
    private String snapshotJson;

    @TableField("snapshot_hash")
    private String snapshotHash;

    @TableField("change_summary")
    private String changeSummary;

    @TableField("publisher")
    private String publisher;

    @TableField("rollback_source_version_id")
    private Long rollbackSourceVersionId;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableLogic(value = "0", delval = "id")
    @TableField("deleted_marker")
    private Long deletedMarker;

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

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(String snapshotJson) {
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

    public Long getDeletedMarker() {
        return deletedMarker;
    }

    public void setDeletedMarker(Long deletedMarker) {
        this.deletedMarker = deletedMarker;
    }
}
