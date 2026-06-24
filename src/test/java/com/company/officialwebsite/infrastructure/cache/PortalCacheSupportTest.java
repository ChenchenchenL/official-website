package com.company.officialwebsite.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * PortalCacheSupportTest：验证 Portal 缓存 key 规则，以及提交后删除与延迟二删的公共行为。
 */
class PortalCacheSupportTest {

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void portalCacheKeyBuilder_shouldBuildPortalNamespaceKey() {
        PortalCacheKeyBuilder keyBuilder = new PortalCacheKeyBuilder("official");

        assertThat(keyBuilder.build("home", "navigation", "banner")).isEqualTo("official:portal:home:navigation:banner");
        assertThat(keyBuilder.build("product", List.of("category", "list"))).isEqualTo("official:portal:product:category:list");
        assertThatThrownBy(() -> keyBuilder.build(" ", "detail"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("module");
    }

    @Test
    void portalCacheInvalidationSupport_shouldDeleteImmediatelyAndScheduleSecondDeleteWithoutTransaction() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        RecordingTaskScheduler taskScheduler = new RecordingTaskScheduler();
        OfficialProperties officialProperties = new OfficialProperties();
        officialProperties.getCache().setSecondDeleteDelay(Duration.ofSeconds(2));
        PortalCacheInvalidationSupport support = new PortalCacheInvalidationSupport(
                redisTemplate,
                taskScheduler,
                new PortalCacheKeyBuilder("official"),
                officialProperties);

        Instant startedAt = Instant.now();
        support.invalidate("official:portal:home", "official:portal:home", "  ", "official:portal:navigation");

        verify(redisTemplate, times(1)).delete(List.of("official:portal:home", "official:portal:navigation"));
        assertThat(taskScheduler.scheduledTasks).hasSize(1);
        assertThat(taskScheduler.scheduledTasks.get(0).executionTime())
                .isAfterOrEqualTo(startedAt.plusSeconds(2))
                .isBeforeOrEqualTo(startedAt.plusSeconds(3));

        taskScheduler.runAll();
        verify(redisTemplate, times(2)).delete(List.of("official:portal:home", "official:portal:navigation"));
    }

    @Test
    void portalCacheInvalidationSupport_shouldRegisterAfterCommitBeforeDeletingCache() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        RecordingTaskScheduler taskScheduler = new RecordingTaskScheduler();
        OfficialProperties officialProperties = new OfficialProperties();
        PortalCacheInvalidationSupport support = new PortalCacheInvalidationSupport(
                redisTemplate,
                taskScheduler,
                new PortalCacheKeyBuilder("official"),
                officialProperties);

        TransactionSynchronizationManager.initSynchronization();
        try {
            support.invalidatePortalKey("home", "navigation");

            verify(redisTemplate, never()).delete(anyCollection());
            assertThat(taskScheduler.scheduledTasks).isEmpty();

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(redisTemplate, times(1)).delete(List.of("official:portal:home:navigation"));
            assertThat(taskScheduler.scheduledTasks).hasSize(1);

            taskScheduler.runAll();
            verify(redisTemplate, times(2)).delete(List.of("official:portal:home:navigation"));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private static final class RecordingTaskScheduler implements TaskScheduler {

        private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
        private final List<ScheduledTask> scheduledTasks = new ArrayList<>();

        @Override
        public Clock getClock() {
            return clock;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, Instant startTime) {
            scheduledTasks.add(new ScheduledTask(task, startTime));
            return null;
        }

        @Override
        public ScheduledFuture<?> schedule(Runnable task, org.springframework.scheduling.Trigger trigger) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Instant startTime, Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleAtFixedRate(Runnable task, Duration period) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Instant startTime, Duration delay) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, Duration delay) {
            throw new UnsupportedOperationException();
        }

        private void runAll() {
            for (ScheduledTask scheduledTask : scheduledTasks) {
                scheduledTask.task().run();
            }
        }
    }

    private record ScheduledTask(Runnable task, Instant executionTime) {
    }
}
