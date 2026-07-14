package com.company.officialwebsite.modules.pagebuilder.model;

import java.time.LocalDateTime;

/**
 * PreviewTokenData：受控预览 Token 在 Redis 中存储的绑定元数据。
 * <p>
 * 存储 pageId、草稿 Hash 快照和创建人，用于后续鉴权和可审计追溯。
 * Token 本身作为 Redis Key 的后缀，不存入此对象，避免循环引用。
 * </p>
 */
public class PreviewTokenData {

    /** 关联的页面定义 ID */
    private Long pageId;

    /** Token 生成时的草稿 schemaHash，可用于幂等判断草稿是否已变更 */
    private String schemaHash;

    /** 生成 Token 的操作员用户名 */
    private String createdBy;

    /** Token 生成时间 */
    private LocalDateTime createdAt;

    public PreviewTokenData() {
    }

    public PreviewTokenData(Long pageId, String schemaHash, String createdBy, LocalDateTime createdAt) {
        this.pageId = pageId;
        this.schemaHash = schemaHash;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public String getSchemaHash() {
        return schemaHash;
    }

    public void setSchemaHash(String schemaHash) {
        this.schemaHash = schemaHash;
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
