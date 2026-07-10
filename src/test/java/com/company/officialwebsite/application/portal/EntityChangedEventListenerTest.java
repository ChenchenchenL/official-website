package com.company.officialwebsite.application.portal;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.company.officialwebsite.infrastructure.event.EntityChangedEvent;
import com.company.officialwebsite.modules.pagebuilder.service.PageCacheInvalidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * EntityChangedEventListenerTest：验证实体变更事件监听器的调用路由与异常隔离能力。
 */
@ExtendWith(MockitoExtension.class)
class EntityChangedEventListenerTest {

    @Mock
    private PageCacheInvalidationService pageCacheInvalidationService;

    private EntityChangedEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new EntityChangedEventListener(pageCacheInvalidationService);
    }

    /**
     * 监听到 product 实体变更事件时，应正确调用 invalidateCacheByTarget 一次，参数匹配。
     */
    @Test
    void onEntityChanged_shouldCallInvalidate_withCorrectParams() {
        EntityChangedEvent event = EntityChangedEvent.of(this, "product", "Product", "42");

        listener.onEntityChanged(event);

        verify(pageCacheInvalidationService, times(1))
                .invalidateCacheByTarget("product", "Product", "42");
    }

    /**
     * 监听到 site 实体变更事件时，参数应正确传递。
     */
    @Test
    void onEntityChanged_shouldCallInvalidate_forSiteConfig() {
        EntityChangedEvent event = EntityChangedEvent.of(this, "site", "SiteConfig", "default");

        listener.onEntityChanged(event);

        verify(pageCacheInvalidationService, times(1))
                .invalidateCacheByTarget("site", "SiteConfig", "default");
    }

    /**
     * 当 invalidateCacheByTarget 抛出异常时，监听器不应向外传播，不应影响主业务流程。
     * 这是防御性设计：缓存失效失败不能回滚已提交的业务事务。
     */
    @Test
    void onEntityChanged_shouldNotPropagateException_whenInvalidateFails() {
        doThrow(new RuntimeException("Redis connection refused"))
                .when(pageCacheInvalidationService)
                .invalidateCacheByTarget(anyString(), anyString(), anyString());

        EntityChangedEvent event = EntityChangedEvent.of(this, "casecenter", "Case", "100");

        // 不应抛出异常
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> listener.onEntityChanged(event));
    }

    /**
     * 监听到 media 实体变更事件时，应正确路由到 invalidateCacheByTarget。
     */
    @Test
    void onEntityChanged_shouldCallInvalidate_forMediaAsset() {
        EntityChangedEvent event = EntityChangedEvent.of(this, "media", "MediaAsset", "2002");

        listener.onEntityChanged(event);

        verify(pageCacheInvalidationService, times(1))
                .invalidateCacheByTarget("media", "MediaAsset", "2002");
    }
}
