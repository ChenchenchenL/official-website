package com.company.officialwebsite.infrastructure.cache;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.trace.TraceContext;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.util.StringUtils;

/**
 * PortalCacheInvalidationSupport：统一承接 Portal 缓存的提交后删除与延迟二次删除动作。
 */
@Component
public class PortalCacheInvalidationSupport {

    private static final Logger log = LoggerFactory.getLogger(PortalCacheInvalidationSupport.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final TaskScheduler taskScheduler;
    private final PortalCacheKeyBuilder portalCacheKeyBuilder;
    private final OfficialProperties officialProperties;

    public PortalCacheInvalidationSupport(
            RedisTemplate<String, Object> redisTemplate,
            TaskScheduler taskScheduler,
            PortalCacheKeyBuilder portalCacheKeyBuilder,
            OfficialProperties officialProperties) {
        this.redisTemplate = redisTemplate;
        this.taskScheduler = taskScheduler;
        this.portalCacheKeyBuilder = portalCacheKeyBuilder;
        this.officialProperties = officialProperties;
    }

    /**
     * 供业务层直接按 portal 语义删除单个 key，避免重复拼接公开缓存路径。
     */
    public void invalidatePortalKey(String module, String... segments) {
        invalidate(portalCacheKeyBuilder.build(module, segments));
    }

    /**
     * Portal 缓存失效必须在事务提交后执行，回滚场景不能提前清空缓存。
     */
    public void invalidate(String... keys) {
        invalidate(Arrays.asList(keys));
    }

    /**
     * 非事务场景也允许复用该组件，例如人工刷新缓存时仍然保持立即删除加二次删除策略。
     */
    public void invalidate(Collection<String> keys) {
        List<String> normalizedKeys = normalizeKeys(keys);
        if (normalizedKeys.isEmpty()) {
            return;
        }

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    deleteNow(normalizedKeys);
                    scheduleSecondDelete(normalizedKeys);
                }
            });
            return;
        }

        deleteNow(normalizedKeys);
        scheduleSecondDelete(normalizedKeys);
    }

    private List<String> normalizeKeys(Collection<String> keys) {
        if (keys == null) {
            return List.of();
        }
        return keys.stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .toList();
    }

    private void deleteNow(List<String> keys) {
        try {
            Long deleted = redisTemplate.delete(keys);
            log.info("portal cache invalidated keys={} deleted={} traceId={}",
                    keys, deleted, TraceContext.getTraceId());
        } catch (Exception ex) {
            log.error("cache invalidation failed keys={} traceId={}", keys, TraceContext.getTraceId(), ex);
        }
    }

    private void scheduleSecondDelete(List<String> keys) {
        try {
            Instant executeAt = Instant.now().plus(officialProperties.getCache().getSecondDeleteDelay());
            taskScheduler.schedule(() -> deleteNow(keys), executeAt);
        } catch (Exception ex) {
            log.error(
                    "schedule delayed cache invalidation failed keys={} delay={} traceId={}",
                    keys,
                    officialProperties.getCache().getSecondDeleteDelay(),
                    TraceContext.getTraceId(),
                    ex);
        }
    }
}
