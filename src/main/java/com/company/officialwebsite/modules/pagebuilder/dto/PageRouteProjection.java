package com.company.officialwebsite.modules.pagebuilder.dto;

/**
 * PageRouteProjection：页面路由投影 DTO，用于缓存联动失效时查询页面的 pageKey 和 routePath。
 * 仅包含缓存 key 组装所需的最小字段，避免加载完整实体。
 */
public class PageRouteProjection {

    private Long id;
    private String pageKey;
    private String routePath;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPageKey() { return pageKey; }
    public void setPageKey(String pageKey) { this.pageKey = pageKey; }

    public String getRoutePath() { return routePath; }
    public void setRoutePath(String routePath) { this.routePath = routePath; }
}
