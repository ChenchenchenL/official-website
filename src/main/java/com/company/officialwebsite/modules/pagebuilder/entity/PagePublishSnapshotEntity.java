package com.company.officialwebsite.modules.pagebuilder.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.company.officialwebsite.common.entity.BaseEntity;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;

/**
 * PagePublishSnapshotEntity: 页面发布快照实体，对应物理表 cms_page_publish_snapshot。
 * 记录每次页面发布生效时的结构快照，用于高频加载渲染前台页面。
 */
@TableName(value = "cms_page_publish_snapshot", autoResultMap = true)
public class PagePublishSnapshotEntity extends BaseEntity {

    /**
     * 关联的页面定义ID
     */
    private Long pageId;

    /**
     * 关联的页面版本ID
     */
    private Long versionId;

    /**
     * 发布快照的 Schema 数据 JSON
     */
    @TableField(value = "snapshot_json", typeHandler = JacksonTypeHandler.class)
    private PageSchemaModel snapshotJson;

    /**
     * 快照哈希校验值
     */
    private String snapshotHash;

    /**
     * 发布快照的状态：ACTIVE-生效中, SUPERSEDED-被覆盖
     */
    private String publishStatus;

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public Long getVersionId() {
        return versionId;
    }

    public void setVersionId(Long versionId) {
        this.versionId = versionId;
    }

    public PageSchemaModel getSnapshotJson() {
        return snapshotJson;
    }

    public void setSnapshotJson(PageSchemaModel snapshotJson) {
        this.snapshotJson = snapshotJson;
    }

    public String getSnapshotHash() {
        return snapshotHash;
    }

    public void setSnapshotHash(String snapshotHash) {
        this.snapshotHash = snapshotHash;
    }

    public String getPublishStatus() {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus) {
        this.publishStatus = publishStatus;
    }
}
