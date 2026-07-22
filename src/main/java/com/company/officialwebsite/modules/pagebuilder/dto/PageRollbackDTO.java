package com.company.officialwebsite.modules.pagebuilder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PageRollbackDTO: 页面回滚请求对象，包含目标版本、草稿乐观锁版本和变更说明。
 */
public class PageRollbackDTO {

    @NotNull(message = "回滚目标版本ID不能为空")
    private Long versionId;

    @NotNull(message = "草稿版本号不能为空")
    private Integer version;

    @NotBlank(message = "变更说明不能为空")
    @Size(max = 255, message = "变更说明最长255字符")
    private String changeSummary;

    private Long expectedSnapshotId;

    private Integer expectedPageVersion;

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public Long getExpectedSnapshotId() {
        return expectedSnapshotId;
    }

    public void setExpectedSnapshotId(Long expectedSnapshotId) {
        this.expectedSnapshotId = expectedSnapshotId;
    }

    public Integer getExpectedPageVersion() {
        return expectedPageVersion;
    }

    public void setExpectedPageVersion(Integer expectedPageVersion) {
        this.expectedPageVersion = expectedPageVersion;
    }
}
