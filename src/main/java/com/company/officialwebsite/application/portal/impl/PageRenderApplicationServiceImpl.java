package com.company.officialwebsite.application.portal.impl;

import com.company.officialwebsite.application.portal.PageBindingResolutionService;
import com.company.officialwebsite.application.portal.PageRenderApplicationService;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.pagebuilder.constants.PageBuilderConstants;
import com.company.officialwebsite.modules.pagebuilder.service.PortalPageRenderService;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageMetaVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalSectionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * PageRenderApplicationServiceImpl: 前台页面渲染装配应用服务实现类。
 */
@Service
public class PageRenderApplicationServiceImpl implements PageRenderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PageRenderApplicationServiceImpl.class);
    private static final String CACHE_MODULE = "PAGE_BUILDER";

    private final PortalPageRenderService portalPageRenderService;
    private final PageBindingResolutionService bindingResolutionService;
    private final PortalCacheSupport portalCacheSupport;

    public PageRenderApplicationServiceImpl(
            PortalPageRenderService portalPageRenderService,
            PageBindingResolutionService bindingResolutionService,
            PortalCacheSupport portalCacheSupport) {
        this.portalPageRenderService = portalPageRenderService;
        this.bindingResolutionService = bindingResolutionService;
        this.portalCacheSupport = portalCacheSupport;
    }

    @Override
    public PortalPageVO renderPageByRoute(String routePath) {
        String cacheKey = PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + routePath.trim();

        // 1. 优先读取已发布的装配后渲染树二级缓存
        PortalPageVO cached = portalCacheSupport.readCache(cacheKey, PortalPageVO.class, CACHE_MODULE);
        if (cached != null) {
            log.info("PageRenderApplication cache hit: routePath={}", routePath);
            return cached;
        }

        // 2. 二级缓存未命中，调用页面构建器服务查询原始快照
        PortalPageVO rawVo = portalPageRenderService.renderPageByRoute(routePath);

        // 3. 遍历区块进行跨模块数据装配
        if (rawVo.getSections() != null) {
            for (PortalSectionVO section : rawVo.getSections()) {
                if (section.getBinding() != null) {
                    try {
                        Object resolvedData = bindingResolutionService.resolveBinding(section.getBinding());
                        section.setBindingData(resolvedData);
                    } catch (Exception e) {
                        log.error("Failed to resolve dynamic data binding for sectionId={}, component={}",
                                section.getId(), section.getComponent(), e);
                    }
                    // 处于安全考虑，向前台输出前清空具体的绑定配置细节
                    section.setBinding(null);
                }
            }
        }

        // 4. 将装配好的完整页面 VO 写入缓存
        portalCacheSupport.writeCache(cacheKey, rawVo, false, CACHE_MODULE);
        log.info("PageRenderApplication cache written: routePath={}", routePath);

        return rawVo;
    }

    @Override
    public PortalPageMetaVO getPageMeta(String pageKey) {
        String cacheKey = PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + pageKey.trim();

        // 1. 优先读取元数据缓存
        PortalPageMetaVO cached = portalCacheSupport.readCache(cacheKey, PortalPageMetaVO.class, CACHE_MODULE);
        if (cached != null) {
            log.info("PageRenderApplication Meta cache hit: pageKey={}", pageKey);
            return cached;
        }

        // 2. 调用服务加载快照元数据
        PortalPageMetaVO metaVo = portalPageRenderService.getPageMeta(pageKey);

        // 3. 写入缓存
        portalCacheSupport.writeCache(cacheKey, metaVo, false, CACHE_MODULE);
        log.info("PageRenderApplication Meta cache written: pageKey={}", pageKey);

        return metaVo;
    }
}
