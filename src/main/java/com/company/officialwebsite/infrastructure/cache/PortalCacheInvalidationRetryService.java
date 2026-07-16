package com.company.officialwebsite.infrastructure.cache;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Portal 缓存删除失败的持久化补偿服务。
 * 任务落库后由所有实例定期争抢执行；删除操作可重入，重复执行不会改变缓存正确性。
 */
@Service
public class PortalCacheInvalidationRetryService {

    private static final Logger log = LoggerFactory.getLogger(PortalCacheInvalidationRetryService.class);
    private static final int BATCH_SIZE = 100;

    private final PortalCacheInvalidationRetryMapper retryMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuditLogService auditLogService;

    public PortalCacheInvalidationRetryService(
            PortalCacheInvalidationRetryMapper retryMapper,
            RedisTemplate<String, Object> redisTemplate,
            AuditLogService auditLogService) {
        this.retryMapper = retryMapper;
        this.redisTemplate = redisTemplate;
        this.auditLogService = auditLogService;
    }

    /** 记录失败删除，供后续实例恢复执行。 */
    public void enqueue(List<String> keys, Exception exception) {
        if (keys == null || keys.isEmpty()) {
            return;
        }
        PortalCacheInvalidationRetryEntity task = new PortalCacheInvalidationRetryEntity();
        task.setCacheKeys(String.join("\n", keys));
        task.setRetryCount(0);
        task.setNextRetryAt(LocalDateTime.now().plusSeconds(30));
        task.setLastError(truncate(exception));
        task.setStatus("PENDING");
        retryMapper.insert(task);
        safeAudit("ENQUEUE_PORTAL_CACHE_INVALIDATION_RETRY", task, exception);
        log.error("portal_cache_invalidation_retry_enqueued taskId={} keyCount={}", task.getId(), keys.size(), exception);
    }

    /** 每分钟处理一次持久化补偿任务，应用重启后仍可继续。 */
    @Scheduled(fixedDelay = 60_000)
    public void retryPendingInvalidations() {
        List<PortalCacheInvalidationRetryEntity> tasks = retryMapper.selectList(
                new LambdaQueryWrapper<PortalCacheInvalidationRetryEntity>()
                        .eq(PortalCacheInvalidationRetryEntity::getStatus, "PENDING")
                        .le(PortalCacheInvalidationRetryEntity::getNextRetryAt, LocalDateTime.now())
                        .orderByAsc(PortalCacheInvalidationRetryEntity::getId)
                        .last("limit " + BATCH_SIZE));
        for (PortalCacheInvalidationRetryEntity task : tasks) {
            retry(task);
        }
    }

    private void retry(PortalCacheInvalidationRetryEntity task) {
        try {
            redisTemplate.delete(toKeys(task.getCacheKeys()));
            task.setStatus("SUCCEEDED");
            task.setLastError(null);
            retryMapper.updateById(task);
            safeAudit("SUCCEED_PORTAL_CACHE_INVALIDATION_RETRY", task, null);
            log.info("portal_cache_invalidation_retry_succeeded taskId={}", task.getId());
        } catch (Exception exception) {
            int attempts = task.getRetryCount() == null ? 1 : task.getRetryCount() + 1;
            task.setRetryCount(attempts);
            task.setLastError(truncate(exception));
            task.setNextRetryAt(LocalDateTime.now().plusSeconds(Math.min(300, 30L * attempts)));
            retryMapper.updateById(task);
            safeAudit("FAIL_PORTAL_CACHE_INVALIDATION_RETRY", task, exception);
            log.error("portal_cache_invalidation_retry_failed taskId={} retryCount={}", task.getId(), attempts, exception);
        }
    }

    private List<String> toKeys(String cacheKeys) {
        return Arrays.stream(cacheKeys.split("\\n")).filter(key -> !key.isBlank()).toList();
    }

    private String truncate(Exception exception) {
        String message = exception.getClass().getSimpleName() + ": " + exception.getMessage();
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private void safeAudit(String action, PortalCacheInvalidationRetryEntity task, Exception exception) {
        try {
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("retryCount", task.getRetryCount());
            payload.put("error", exception == null ? null : truncate(exception));
            auditLogService.recordGenericOperation("CACHE", action, "PORTAL_CACHE_INVALIDATION", task.getId(), null,
                    payload);
        } catch (Exception auditException) {
            log.error("portal cache retry audit failed taskId={}", task.getId(), auditException);
        }
    }
}
