package com.company.officialwebsite.modules.pagebuilder.vo;

import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;

import java.time.LocalDateTime;

/**
 * PageDraftVO：页面草稿详情返回对象，供前端构建编辑器初始状态使用。
 */
public class PageDraftVO {

    /** 草稿主键 ID */
    private Long id;

    /** 关联页面定义 ID */
    private Long pageId;

    /**
     * 页面完整配置的 Schema 数据，初次创建草稿时可为 null（尚未编辑）。
     */
    private PageSchemaModel schemaJson;

    /** Schema 数据的 SHA-256 哈希校验值 */
    private String schemaHash;

    /** 当前编辑会话备注/变更说明 */
    private String editorSessionRemark;

    /** 乐观锁版本号，前端保存时须回传 */
    private Integer version;

    /** 最近更新时间 */
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public PageSchemaModel getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(PageSchemaModel schemaJson) {
        this.schemaJson = schemaJson;
    }

    public String getSchemaHash() {
        return schemaHash;
    }

    public void setSchemaHash(String schemaHash) {
        this.schemaHash = schemaHash;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
