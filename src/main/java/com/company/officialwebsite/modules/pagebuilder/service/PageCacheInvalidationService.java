package com.company.officialwebsite.modules.pagebuilder.service;

/**
 * PageCacheInvalidationService：页面缓存联动失效服务接口。
 * 根据外部实体变更事件，查找依赖该实体的页面，并在事务提交后清理对应缓存。
 */
public interface PageCacheInvalidationService {

    /**
     * 根据变更的目标实体，失效所有依赖该实体的页面缓存。
     *
     * @param module     业务模块标识，如 "product", "casecenter"
     * @param entityType 实体类型标识，如 "Product", "Case"
     * @param entityId   实体主键（字符串形式）
     */
    void invalidateCacheByTarget(String module, String entityType, String entityId);

    /**
     * 失效指定页面及其关联页面的渲染和 SEO 缓存。
     *
     * @param pageId 已发布或回滚的页面 ID
     */
    void invalidatePageAndRelatedCaches(Long pageId);

    /**
     * 失效指定页面的 Portal 渲染与 SEO 缓存，支持路由变更时同时清理旧路由与新路由 Key。
     *
     * @param pageId       页面 ID（可为 null，若非空则同步计算关联受影响页面）
     * @param oldRoutePath 变更前的旧路由路径（可为 null）
     * @param newRoutePath 变更后的新路由路径（可为 null）
     */
    void invalidatePageCaches(Long pageId, String oldRoutePath, String newRoutePath);
}
