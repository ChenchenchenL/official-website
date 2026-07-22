package com.company.officialwebsite.modules.pagebuilder.vo;

import java.time.LocalDateTime;

/**
 * PortalRouteVO：Portal 动态路由清单视图对象。
 */
public class PortalRouteVO {

    private String routePath;
    private String pageKey;
    private String name;
    private Boolean visible;
    private LocalDateTime updatedAt;

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

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

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
