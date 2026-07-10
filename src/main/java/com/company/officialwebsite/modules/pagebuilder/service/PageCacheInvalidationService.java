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
}
