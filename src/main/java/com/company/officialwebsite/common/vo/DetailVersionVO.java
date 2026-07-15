package com.company.officialwebsite.common.vo;

import java.time.LocalDateTime;

/**
 * DetailVersionVO：详情历史发布版本统一响应 VO。
 */
public class DetailVersionVO {

    private Long id;
    private Long resourceId;
    private Integer versionNo;
    private String changeSummary;
    private String publisher;
    private LocalDateTime publishedAt;

    public DetailVersionVO() {
    }

    public DetailVersionVO(Long id, Long resourceId, Integer versionNo, String changeSummary, String publisher, LocalDateTime publishedAt) {
        this.id = id;
        this.resourceId = resourceId;
        this.versionNo = versionNo;
        this.changeSummary = changeSummary;
        this.publisher = publisher;
        this.publishedAt = publishedAt;
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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
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

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
