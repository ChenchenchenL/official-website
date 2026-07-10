package com.company.officialwebsite.modules.pagebuilder.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.company.officialwebsite.common.entity.BaseEntity;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;

/**
 * PageDraftEntity: 页面草稿实体，对应物理表 cms_page_draft。
 * 暂存页面构建设计器的最新编辑内容，支持未发布前的持续保存。
 */
@TableName(value = "cms_page_draft", autoResultMap = true)
public class PageDraftEntity extends BaseEntity {

    /**
     * 关联的页面定义ID
     */
    private Long pageId;

    /**
     * 页面完整配置的 Schema 数据 JSON
     */
    @TableField(value = "schema_json", typeHandler = JacksonTypeHandler.class)
    private PageSchemaModel schemaJson;

    /**
     * 页面 Schema 数据的哈希校验值，用于变化比对
     */
    private String schemaHash;

    /**
     * 当前编辑会话备注/变更说明
     */
    private String editorSessionRemark;

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
}
