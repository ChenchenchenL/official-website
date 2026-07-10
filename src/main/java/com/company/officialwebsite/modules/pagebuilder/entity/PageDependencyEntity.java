package com.company.officialwebsite.modules.pagebuilder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * PageDependencyEntity: 页面数据依赖记录实体，对应物理表 cms_page_dependency。
 * 显式记录页面组件对外部业务数据或媒体资源的依赖关系，用于级联检查与删除风险校验。
 */
@TableName("cms_page_dependency")
public class PageDependencyEntity extends BaseEntity {

    /**
     * 关联的页面定义ID
     */
    private Long pageId;

    /**
     * 关联的发布快照ID
     */
    private Long snapshotId;

    /**
     * 页面内组件实例唯一ID
     */
    private String componentInstanceId;

    /**
     * 依赖的数据类型：MEDIA-媒体, ENTITY-业务实体
     */
    private String dependencyType;

    /**
     * 依赖的业务模块，如 "product", "casecenter"
     */
    private String targetModule;

    /**
     * 依赖的目标实体类型，如 "ProductEntity", "CaseEntity"
     */
    private String targetEntityType;

    /**
     * 依赖的目标实体唯一主键/标识
     */
    private String targetEntityId;

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public Long getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(Long snapshotId) {
        this.snapshotId = snapshotId;
    }

    public String getComponentInstanceId() {
        return componentInstanceId;
    }

    public void setComponentInstanceId(String componentInstanceId) {
        this.componentInstanceId = componentInstanceId;
    }

    public String getDependencyType() {
        return dependencyType;
    }

    public void setDependencyType(String dependencyType) {
        this.dependencyType = dependencyType;
    }

    public String getTargetModule() {
        return targetModule;
    }

    public void setTargetModule(String targetModule) {
        this.targetModule = targetModule;
    }

    public String getTargetEntityType() {
        return targetEntityType;
    }

    public void setTargetEntityType(String targetEntityType) {
        this.targetEntityType = targetEntityType;
    }

    public String getTargetEntityId() {
        return targetEntityId;
    }

    public void setTargetEntityId(String targetEntityId) {
        this.targetEntityId = targetEntityId;
    }
}
