package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.pagebuilder.constants.PageBuilderConstants;
import com.company.officialwebsite.modules.pagebuilder.dto.PageRouteProjection;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDependencyMapper;
import com.company.officialwebsite.modules.pagebuilder.service.PageCacheInvalidationService;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * PageCacheInvalidationServiceImpl：页面缓存联动失效服务实现。
 *
 * <p>工作流程：
 * <ol>
 *   <li>通过 {@code PageDependencyMapper} 查询依赖目标实体的页面 ID 列表；</li>
 *   <li>通过 {@code PageDefinitionMapper} 查询对应的 pageKey 与 routePath；</li>
 *   <li>组装缓存 key 集合并调用 {@code PortalCacheSupport.invalidate} 触发事务后双重失效。</li>
 * </ol>
 */
@Service
public class PageCacheInvalidationServiceImpl implements PageCacheInvalidationService {

    private static final Logger log = LoggerFactory.getLogger(PageCacheInvalidationServiceImpl.class);

    private final PageDependencyMapper pageDependencyMapper;
    private final PageDefinitionMapper pageDefinitionMapper;
    private final PortalCacheSupport portalCacheSupport;

    public PageCacheInvalidationServiceImpl(
            PageDependencyMapper pageDependencyMapper,
            PageDefinitionMapper pageDefinitionMapper,
            PortalCacheSupport portalCacheSupport) {
        this.pageDependencyMapper = pageDependencyMapper;
        this.pageDefinitionMapper = pageDefinitionMapper;
        this.portalCacheSupport = portalCacheSupport;
    }

    @Override
    public void invalidateCacheByTarget(String module, String entityType, String entityId) {
        // 1. 查询依赖该实体的所有 pageId
        List<Long> pageIds = pageDependencyMapper.selectPageIdsByTarget(module, entityType, entityId);
        if (pageIds == null || pageIds.isEmpty()) {
            log.debug("[PageCacheInvalidation] no dependent pages found for module={} entityType={} entityId={}",
                    module, entityType, entityId);
            return;
        }

        // 2. 查询页面的 pageKey 和 routePath
        List<PageRouteProjection> routes = pageDefinitionMapper.selectRoutesByPageIds(pageIds);
        if (routes == null || routes.isEmpty()) {
            log.warn("[PageCacheInvalidation] pageIds found but no route projections, pageIds={}", pageIds);
            return;
        }

        // 3. 组装需要失效的缓存 key
        List<String> keys = new ArrayList<>(routes.size() * 2);
        for (PageRouteProjection route : routes) {
            keys.add(PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + route.getRoutePath());
            keys.add(PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + route.getPageKey());
        }

        // 4. 触发事务后失效（含延迟二次删除）
        portalCacheSupport.invalidate(keys.toArray(new String[0]));
        log.info("[PageCacheInvalidation] invalidated {} page cache(s) for module={} entityType={} entityId={} pageIds={}",
                routes.size(), module, entityType, entityId, pageIds);
    }
}
