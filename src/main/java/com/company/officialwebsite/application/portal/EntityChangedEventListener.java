package com.company.officialwebsite.application.portal;

import com.company.officialwebsite.infrastructure.event.EntityChangedEvent;
import com.company.officialwebsite.modules.pagebuilder.service.PageCacheInvalidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * EntityChangedEventListener：实体变更事件监听器。
 *
 * <p>在业务实体（产品、案例、站点配置、媒体等）发生更新或删除后，
 * 订阅 {@link EntityChangedEvent} 并在事务提交后（AFTER_COMMIT）异步触发
 * {@link PageCacheInvalidationService#invalidateCacheByTarget}，
 * 清理依赖该实体的已发布页面缓存。
 *
 * <p>异步执行（@Async）确保不阻塞主业务流程；内部异常仅记录日志，不向外传播。
 */
@Component
public class EntityChangedEventListener {

    private static final Logger log = LoggerFactory.getLogger(EntityChangedEventListener.class);

    private final PageCacheInvalidationService pageCacheInvalidationService;

    public EntityChangedEventListener(PageCacheInvalidationService pageCacheInvalidationService) {
        this.pageCacheInvalidationService = pageCacheInvalidationService;
    }

    /**
     * 监听实体变更事件，在事务提交后异步执行页面缓存联动失效。
     *
     * <p>使用 {@code AFTER_COMMIT} 确保只有在数据库事务成功提交后才执行缓存清理，
     * 避免事务回滚后误删有效缓存；使用 {@code @Async} 避免阻塞业务线程。
     *
     * @param event 实体变更事件
     */
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEntityChanged(EntityChangedEvent event) {
        try {
            log.info("[EntityChangedEvent] received module={} entityType={} entityId={}",
                    event.getTargetModule(), event.getTargetEntityType(), event.getTargetEntityId());
            pageCacheInvalidationService.invalidateCacheByTarget(
                    event.getTargetModule(),
                    event.getTargetEntityType(),
                    event.getTargetEntityId());
        } catch (Exception e) {
            // 缓存失效失败不应影响主业务，仅记录错误日志
            log.error("[EntityChangedEvent] cache invalidation failed module={} entityType={} entityId={}",
                    event.getTargetModule(), event.getTargetEntityType(), event.getTargetEntityId(), e);
        }
    }
}
