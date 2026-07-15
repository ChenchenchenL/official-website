package com.company.officialwebsite.modules.product.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * ProductVersionVO：产品发布版本 VO。
 */
public class ProductVersionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long productId;
    private Integer versionNo;
    private Object snapshotJson;
    private String snapshotHash;
    private String changeSummary;
    private String publisher;
    private Long rollbackSourceVersionId;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    public ProductVersionVO() {
    }

    public ProductVersionVO(Long id, Long productId, Integer versionNo, Object snapshotJson, String snapshotHash, String changeSummary, String publisher, Long rollbackSourceVersionId, LocalDateTime publishedAt, LocalDateTime createdAt) {
        this.id = id;
        this.productId = productId;
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

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
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
