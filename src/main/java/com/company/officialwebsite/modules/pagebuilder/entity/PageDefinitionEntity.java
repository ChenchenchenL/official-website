package com.company.officialwebsite.modules.pagebuilder.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * PageDefinitionEntity: 页面定义实体，对应物理表 cms_page_definition。
 * 承载页面的访问路由、Key标识、页面类型和启用状态等元数据信息。
 */
@TableName("cms_page_definition")
public class PageDefinitionEntity extends BaseEntity {

    /**
     * 页面唯一Key标识
     */
    private String pageKey;

    /**
     * 页面名称
     */
    private String name;

    /**
     * 页面访问路由路径
     */
    private String routePath;

    /**
     * 页面类型：NORMAL-普通页面, SYSTEM-系统内置页面
     */
    private String pageType;

    /**
     * 页面启用状态：ENABLED-启用, DISABLED-禁用
     */
    private String status;

    /**
     * 是否前台可见：true-显示，false-隐藏
     */
    private Boolean visible;

    /**
     * 排序值，越小越靠前
     */
    private Integer sortOrder;

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getPageType() {
        return pageType;
    }

    public void setPageType(String pageType) {
        this.pageType = pageType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
