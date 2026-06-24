package com.company.officialwebsite.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.trace.TraceConstants;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * AsyncConfigurationTest：验证统一异步线程池参数、上下文透传与线程复用清理行为。
 */
class AsyncConfigurationTest {

    @AfterEach
    void tearDown() {
        MDC.clear();
        SecurityContextHolder.clearContext();
    }

    @Test
    void officialAsyncExecutor_shouldApplyPoolSettingsAndCallerRunsPolicy() {
        ThreadPoolTaskExecutor executor = buildExecutor();
        try {
            assertThat(executor.getThreadNamePrefix()).isEqualTo("test-official-async-");
            assertThat(executor.getThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
            assertThat(executor.getThreadPoolExecutor().getMaximumPoolSize()).isEqualTo(1);
            assertThat(executor.getThreadPoolExecutor().getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(45L);
            assertThat(executor.getThreadPoolExecutor().getQueue().remainingCapacity()).isEqualTo(5);
            assertThat(executor.getThreadPoolExecutor().getRejectedExecutionHandler())
                    .isInstanceOf(ThreadPoolExecutor.CallerRunsPolicy.class);
            assertThat(ReflectionTestUtils.getField(executor, "waitForTasksToCompleteOnShutdown")).isEqualTo(true);
            assertThat(ReflectionTestUtils.getField(executor, "awaitTerminationMillis")).isEqualTo(15000L);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void officialAsyncExecutor_shouldPropagateAndCleanMdcAndSecurityContext() throws Exception {
        ThreadPoolTaskExecutor executor = buildExecutor();
        try {
            MDC.put(TraceConstants.TRACE_ID, "trace-123");
            SecurityContextHolder.getContext().setAuthentication(
                    UsernamePasswordAuthenticationToken.authenticated("operator", "N/A", List.of()));

            Future<AsyncTaskSnapshot> firstTask = executor.submit(() -> new AsyncTaskSnapshot(
                    Thread.currentThread().getName(),
                    MDC.get(TraceConstants.TRACE_ID),
                    SecurityContextHolder.getContext().getAuthentication() == null
                            ? null
                            : SecurityContextHolder.getContext().getAuthentication().getName()));

            AsyncTaskSnapshot firstSnapshot = firstTask.get();
            assertThat(firstSnapshot.threadName()).startsWith("test-official-async-");
            assertThat(firstSnapshot.traceId()).isEqualTo("trace-123");
            assertThat(firstSnapshot.authenticationName()).isEqualTo("operator");

            MDC.clear();
            SecurityContextHolder.clearContext();

            Future<AsyncTaskSnapshot> secondTask = executor.submit(() -> new AsyncTaskSnapshot(
                    Thread.currentThread().getName(),
                    MDC.get(TraceConstants.TRACE_ID),
                    SecurityContextHolder.getContext().getAuthentication() == null
                            ? null
                            : SecurityContextHolder.getContext().getAuthentication().getName()));

            AsyncTaskSnapshot secondSnapshot = secondTask.get();
            assertThat(secondSnapshot.threadName()).isEqualTo(firstSnapshot.threadName());
            assertThat(secondSnapshot.traceId()).isNull();
            assertThat(secondSnapshot.authenticationName()).isNull();
        } finally {
            executor.shutdown();
        }
    }

    private ThreadPoolTaskExecutor buildExecutor() {
        OfficialProperties officialProperties = new OfficialProperties();
        officialProperties.getAsync().setCorePoolSize(1);
        officialProperties.getAsync().setMaxPoolSize(1);
        officialProperties.getAsync().setQueueCapacity(5);
        officialProperties.getAsync().setKeepAlive(Duration.ofSeconds(45));
        officialProperties.getAsync().setAwaitTermination(Duration.ofSeconds(15));
        officialProperties.getAsync().setThreadNamePrefix("test-official-async-");

        AsyncConfiguration configuration = new AsyncConfiguration(officialProperties, null);
        ThreadPoolTaskExecutor executor = configuration.officialAsyncExecutor();
        executor.initialize();
        return executor;
    }

    private record AsyncTaskSnapshot(String threadName, String traceId, String authenticationName) {
    }
}
