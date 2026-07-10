package com.company.officialwebsite.modules.pagebuilder.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.company.officialwebsite.common.entity.BaseEntity;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;

/**
 * PageVersionEntity: 页面版本实体，对应物理表 cms_page_version。
 * 记录页面设计发布或手动保存的历史备份版本。
 */
@TableName(value = "cms_page_version", autoResultMap = true)
public class PageVersionEntity extends BaseEntity {

    /**
     * 关联的页面定义ID
     */
    private Long pageId;

    /**
     * 版本号/版本序号
     */
    private Integer versionNo;

    /**
     * 版本来源类型：MANUAL_SAVE-手动保存, PUBLISH_BASE-发布备份, ROLLBACK_BASE-回滚备份
     */
    private String sourceType;

    /**
     * 页面完整配置的 Schema 数据 JSON
     */
    @TableField(value = "schema_json", typeHandler = JacksonTypeHandler.class)
    private PageSchemaModel schemaJson;

    /**
     * 页面 Schema 数据的哈希校验值，用于差异比对
     */
    private String schemaHash;

    /**
     * 版本变更描述/摘要说明
     */
    private String changeSummary;

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
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

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }
}
