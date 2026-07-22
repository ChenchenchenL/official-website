package com.company.officialwebsite.modules.pagebuilder.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.pagebuilder.constants.PageBuilderConstants;
import com.company.officialwebsite.modules.pagebuilder.dto.PageRouteProjection;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDependencyMapper;
import com.company.officialwebsite.modules.pagebuilder.service.impl.PageCacheInvalidationServiceImpl;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PageCacheInvalidationServiceTest：验证页面缓存联动失效服务的核心分支逻辑。
 */
@ExtendWith(MockitoExtension.class)
class PageCacheInvalidationServiceTest {

    @Mock
    private PageDependencyMapper pageDependencyMapper;

    @Mock
    private PageDefinitionMapper pageDefinitionMapper;

    @Mock
    private PortalCacheSupport portalCacheSupport;

    private PageCacheInvalidationService service;

    @BeforeEach
    void setUp() {
        service = new PageCacheInvalidationServiceImpl(
                pageDependencyMapper, pageDefinitionMapper, portalCacheSupport);
    }

    /**
     * 当依赖 Mapper 返回空列表时，不应调用 portalCacheSupport.invalidate。
     */
    @Test
    void invalidateCacheByTarget_shouldSkip_whenNoDependentPages() {
        when(pageDependencyMapper.selectPageIdsByTarget(anyString(), anyString(), anyString()))
                .thenReturn(Collections.emptyList());

        service.invalidateCacheByTarget("product", "Product", "1");

        verify(portalCacheSupport, never()).invalidate(any(String[].class));
        verify(pageDefinitionMapper, never()).selectRoutesByPageIds(any());
    }

    /**
     * 当依赖 Mapper 返回 null 时（防御性），不应调用 invalidate。
     */
    @Test
    void invalidateCacheByTarget_shouldSkip_whenMapperReturnsNull() {
        when(pageDependencyMapper.selectPageIdsByTarget(anyString(), anyString(), anyString()))
                .thenReturn(null);

        service.invalidateCacheByTarget("casecenter", "Case", "42");

        verify(portalCacheSupport, never()).invalidate(any(String[].class));
    }

    /**
     * 当有单个依赖页面时，应正确组装两个缓存 key 并调用 invalidate。
     */
    @Test
    void invalidateCacheByTarget_shouldInvalidateTwoKeys_whenSinglePageFound() {
        when(pageDependencyMapper.selectPageIdsByTarget("product", "Product", "1"))
                .thenReturn(List.of(100L));

        PageRouteProjection projection = new PageRouteProjection();
        projection.setId(100L);
        projection.setPageKey("home-page");
        projection.setRoutePath("/home");
        when(pageDefinitionMapper.selectRoutesByPageIds(List.of(100L)))
                .thenReturn(List.of(projection));

        service.invalidateCacheByTarget("product", "Product", "1");

        ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
        verify(portalCacheSupport, times(1)).invalidate(keysCaptor.capture());

        String[] actualKeys = keysCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(2, actualKeys.length);
        org.junit.jupiter.api.Assertions.assertEquals(
                PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + "/home", actualKeys[0]);
        org.junit.jupiter.api.Assertions.assertEquals(
                PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + "home-page", actualKeys[1]);
    }

    /** 案例发布、回滚或下线时，应按案例依赖清理页面渲染与 SEO 缓存。 */
    @Test
    void invalidateCacheByTarget_shouldInvalidateCasePageRenderAndSeoCaches() {
        when(pageDependencyMapper.selectPageIdsByTarget("case", "Case", "42"))
                .thenReturn(List.of(42L));

        PageRouteProjection projection = new PageRouteProjection();
        projection.setId(42L);
        projection.setPageKey("case-detail");
        projection.setRoutePath("/cases/42");
        when(pageDefinitionMapper.selectRoutesByPageIds(List.of(42L)))
                .thenReturn(List.of(projection));

        service.invalidateCacheByTarget("case", "Case", "42");

        ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
        verify(portalCacheSupport).invalidate(keysCaptor.capture());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{
                PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + "/cases/42",
                PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + "case-detail"
        }, keysCaptor.getValue());
    }

    /**
     * 当有多个依赖页面时，所有页面的缓存 key 均应被失效（单次调用）。
     */
    @Test
    void invalidateCacheByTarget_shouldInvalidateAllKeys_whenMultiplePagesFound() {
        when(pageDependencyMapper.selectPageIdsByTarget("site", "SiteConfig", "default"))
                .thenReturn(Arrays.asList(10L, 20L, 30L));

        PageRouteProjection p1 = new PageRouteProjection();
        p1.setId(10L); p1.setPageKey("home-page"); p1.setRoutePath("/home");
        PageRouteProjection p2 = new PageRouteProjection();
        p2.setId(20L); p2.setPageKey("about-page"); p2.setRoutePath("/about");
        PageRouteProjection p3 = new PageRouteProjection();
        p3.setId(30L); p3.setPageKey("careers-page"); p3.setRoutePath("/careers");

        when(pageDefinitionMapper.selectRoutesByPageIds(Arrays.asList(10L, 20L, 30L)))
                .thenReturn(Arrays.asList(p1, p2, p3));

        service.invalidateCacheByTarget("site", "SiteConfig", "default");

        ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
        verify(portalCacheSupport, times(1)).invalidate(keysCaptor.capture());

        String[] actualKeys = keysCaptor.getValue();
        // 3 pages × 2 keys each = 6 keys
        org.junit.jupiter.api.Assertions.assertEquals(6, actualKeys.length);
    }

    /**
     * 页面发布或回滚时，应查询关联页面并一次性失效当前页及关联页的渲染、SEO 缓存。
     */
    @Test
    void invalidatePageAndRelatedCaches_shouldInvalidateCurrentAndRelatedPageKeys() {
        when(pageDependencyMapper.selectRelatedPageIds(10L)).thenReturn(List.of(20L, 30L));

        PageRouteProjection current = new PageRouteProjection();
        current.setId(10L);
        current.setPageKey("home-page");
        current.setRoutePath("/");
        PageRouteProjection relatedOne = new PageRouteProjection();
        relatedOne.setId(20L);
        relatedOne.setPageKey("product-page");
        relatedOne.setRoutePath("/products");
        PageRouteProjection relatedTwo = new PageRouteProjection();
        relatedTwo.setId(30L);
        relatedTwo.setPageKey("case-page");
        relatedTwo.setRoutePath("/cases");
        when(pageDefinitionMapper.selectRoutesByPageIds(any()))
                .thenReturn(List.of(current, relatedOne, relatedTwo));

        service.invalidatePageAndRelatedCaches(10L);

        verify(pageDefinitionMapper).selectRoutesByPageIds(argThat(pageIds ->
                pageIds.size() == 3 && pageIds.containsAll(List.of(10L, 20L, 30L))));
        ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
        verify(portalCacheSupport).invalidate(keysCaptor.capture());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[]{
                PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + "/",
                PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + "home-page",
                PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + "/products",
                PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + "product-page",
                PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + "/cases",
                PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + "case-page"
        }, keysCaptor.getValue());
    }

    /**
     * 当路由发生变更时，应当同时清理旧路由与新路由的 Portal 缓存。
     */
    @Test
    void invalidatePageCaches_shouldInvalidateBothOldAndNewRoutes_whenRouteChangeOccurs() {
        PageRouteProjection p = new PageRouteProjection();
        p.setId(1L);
        p.setPageKey("about-key");
        p.setRoutePath("/about-new");

        when(pageDefinitionMapper.selectRoutesByPageIds(any()))
                .thenReturn(List.of(p));

        service.invalidatePageCaches(1L, "/about-old", "/about-new");

        ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
        verify(portalCacheSupport).invalidate(keysCaptor.capture());

        String[] actualKeys = keysCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertTrue(Arrays.asList(actualKeys).contains(PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + "/about-old"));
        org.junit.jupiter.api.Assertions.assertTrue(Arrays.asList(actualKeys).contains(PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + "/about-new"));
        org.junit.jupiter.api.Assertions.assertTrue(Arrays.asList(actualKeys).contains(PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + "about-key"));
    }

    /**
     * 当路由未变更时，正确触发单一路由和元数据缓存清理。
     */
    @Test
    void invalidatePageCaches_shouldInvalidateSingleRoute_whenNoRouteChange() {
        PageRouteProjection p = new PageRouteProjection();
        p.setId(2L);
        p.setPageKey("contact-key");
        p.setRoutePath("/contact");

        when(pageDefinitionMapper.selectRoutesByPageIds(any()))
                .thenReturn(List.of(p));

        service.invalidatePageCaches(2L, "/contact", "/contact");

        ArgumentCaptor<String[]> keysCaptor = ArgumentCaptor.forClass(String[].class);
        verify(portalCacheSupport).invalidate(keysCaptor.capture());

        String[] actualKeys = keysCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(2, actualKeys.length);
        org.junit.jupiter.api.Assertions.assertEquals(PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + "/contact", actualKeys[0]);
        org.junit.jupiter.api.Assertions.assertEquals(PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + "contact-key", actualKeys[1]);
    }
}
