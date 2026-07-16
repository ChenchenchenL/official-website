package com.company.officialwebsite.infrastructure.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.modules.site.vo.PortalHonorVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
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

    @Test
    void portalCacheInvalidationSupport_shouldPersistRetryTask_whenRedisDeleteFails() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        RecordingTaskScheduler taskScheduler = new RecordingTaskScheduler();
        PortalCacheInvalidationRetryService retryService = mock(PortalCacheInvalidationRetryService.class);
        OfficialProperties officialProperties = new OfficialProperties();
        PortalCacheInvalidationSupport support = new PortalCacheInvalidationSupport(
                redisTemplate, taskScheduler, new PortalCacheKeyBuilder("official"), officialProperties, retryService);
        List<String> keys = List.of("official:portal:products");
        doThrow(new IllegalStateException("redis unavailable")).when(redisTemplate).delete(keys);

        support.invalidate(keys);

        verify(retryService).enqueue(eq(keys), any(IllegalStateException.class));
        assertThat(taskScheduler.scheduledTasks).hasSize(1);
    }

    @Test
    void readCache_shouldReturnValue_whenCacheHit() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("official:portal:honors")).thenReturn(Map.of("name", "国家高新技术企业"));
        PortalCacheSupport support = newPortalCacheSupport(redisTemplate);

        PortalHonorVO result = support.readCache("official:portal:honors", PortalHonorVO.class, "honors");

        assertThat(result.getName()).isEqualTo("国家高新技术企业");
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    void readCache_shouldReturnNull_whenCacheMiss() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("official:portal:honors")).thenReturn(null);
        PortalCacheSupport support = newPortalCacheSupport(redisTemplate);

        PortalHonorVO result = support.readCache("official:portal:honors", PortalHonorVO.class, "honors");

        assertThat(result).isNull();
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    void readCache_shouldDeleteBadCacheAndReturnNull_whenDeserializeFails() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("official:portal:honors")).thenReturn("not-an-object");
        PortalCacheSupport support = newPortalCacheSupport(redisTemplate);

        PortalHonorVO result = support.readCache("official:portal:honors", PortalHonorVO.class, "honors");

        assertThat(result).isNull();
        verify(redisTemplate, times(1)).delete("official:portal:honors");
    }

    @Test
    void readListCache_shouldDeleteBadCacheAndReturnNull_whenDeserializeFails() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("official:portal:honors")).thenReturn("not-a-list");
        PortalCacheSupport support = newPortalCacheSupport(redisTemplate);

        List<PortalHonorVO> result = support.readListCache("official:portal:honors", PortalHonorVO.class, "honors");

        assertThat(result).isNull();
        verify(redisTemplate, times(1)).delete("official:portal:honors");
    }

    @Test
    void readCache_shouldReturnNull_whenRedisReadFails() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("redis down"));
        PortalCacheSupport support = newPortalCacheSupport(redisTemplate);

        PortalHonorVO result = support.readCache("official:portal:honors", PortalHonorVO.class, "honors");

        assertThat(result).isNull();
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    void readCache_shouldDeleteBadCache_whenRedisThrowsSerializationException() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        when(redisTemplate.opsForValue())
                .thenThrow(new SerializationException("corrupt bytes", new RuntimeException("malformed json")));
        PortalCacheSupport support = newPortalCacheSupport(redisTemplate);

        PortalHonorVO result = support.readCache("official:portal:honors", PortalHonorVO.class, "honors");

        assertThat(result).isNull();
        // 损坏字节触发的 SerializationException 属于数据损坏，应主动删除坏缓存以实现自愈。
        verify(redisTemplate, times(1)).delete("official:portal:honors");
    }

    @Test
    void genericJackson2JsonRedisSerializer_shouldThrowSerializationExceptionOnCorruptBytes() {
        // 真实序列化器读取损坏字节时抛 SerializationException，这正是 readCache 第一个 catch 需识别并清理坏缓存的触发条件。
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer();
        byte[] corruptBytes = "{not-json".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> serializer.deserialize(corruptBytes))
                .isInstanceOf(SerializationException.class);
    }

    @Test
    void writeCache_shouldUseEmptyResultTtl_whenResultIsEmpty() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        PortalCacheSupport support = newPortalCacheSupport(redisTemplate);

        support.writeCache("official:portal:honors", Collections.emptyList(), true, "honors");

        verify(valueOperations).set(eq("official:portal:honors"), any(), eq(Duration.ofMinutes(1)));
    }

    @Test
    void writeCache_shouldUseModuleTtl_whenResultIsNotEmpty() {
        RedisTemplate<String, Object> redisTemplate = mock(RedisTemplate.class);
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        PortalCacheSupport support = newPortalCacheSupport(redisTemplate);

        support.writeCache("official:portal:honors", List.of(new PortalHonorVO()), false, "honors");

        verify(valueOperations).set(eq("official:portal:honors"), any(), eq(Duration.ofMinutes(10)));
    }

    @Test
    void resolveTtl_shouldUseOverride_whenModuleConfigured() {
        OfficialProperties properties = new OfficialProperties();
        properties.getCache().setDefaultTtl(Duration.ofMinutes(10));
        properties.getCache().setPortalTtlOverrides(Map.of("honors", Duration.ofMinutes(30)));
        PortalCacheSupport support = new PortalCacheSupport(
                mock(RedisTemplate.class), new PortalCacheKeyBuilder("official"),
                mock(PortalCacheInvalidationSupport.class), properties, new ObjectMapper().registerModule(new JavaTimeModule()));

        assertThat(support.resolveTtl("honors")).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    void isEmptyResult_shouldDetectVariousEmptyValues() {
        PortalCacheSupport support = newPortalCacheSupport(mock(RedisTemplate.class));

        assertThat(support.isEmptyResult(null)).isTrue();
        assertThat(support.isEmptyResult("")).isTrue();
        assertThat(support.isEmptyResult(Collections.emptyList())).isTrue();
        assertThat(support.isEmptyResult(Collections.emptyMap())).isTrue();
        assertThat(support.isEmptyResult("data")).isFalse();
        assertThat(support.isEmptyResult(List.of("item"))).isFalse();
    }

    @Test
    void invalidatePortalKey_shouldDelegateToInvalidationSupport() {
        PortalCacheInvalidationSupport invalidationSupport = mock(PortalCacheInvalidationSupport.class);
        OfficialProperties properties = new OfficialProperties();
        PortalCacheSupport support = new PortalCacheSupport(
                mock(RedisTemplate.class), new PortalCacheKeyBuilder("official"),
                invalidationSupport, properties, new ObjectMapper().registerModule(new JavaTimeModule()));

        support.invalidatePortalKey("honors");

        verify(invalidationSupport).invalidatePortalKey("honors");
    }

    private PortalCacheSupport newPortalCacheSupport(RedisTemplate<String, Object> redisTemplate) {
        OfficialProperties properties = new OfficialProperties();
        properties.getCache().setDefaultTtl(Duration.ofMinutes(10));
        properties.getCache().setEmptyResultTtl(Duration.ofMinutes(1));
        return new PortalCacheSupport(
                redisTemplate,
                new PortalCacheKeyBuilder("official"),
                mock(PortalCacheInvalidationSupport.class),
                properties,
                new ObjectMapper().registerModule(new JavaTimeModule()));
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
