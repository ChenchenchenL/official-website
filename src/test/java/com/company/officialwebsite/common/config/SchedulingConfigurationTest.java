package com.company.officialwebsite.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * SchedulingConfigurationTest：验证统一调度线程池的参数、关闭行为和任务可执行性。
 */
class SchedulingConfigurationTest {

    @Test
    void officialTaskScheduler_shouldApplyPoolSettingsAndExecuteScheduledTask() throws Exception {
        OfficialProperties officialProperties = new OfficialProperties();
        officialProperties.getScheduler().setPoolSize(1);
        officialProperties.getScheduler().setAwaitTermination(Duration.ofSeconds(15));
        officialProperties.getScheduler().setThreadNamePrefix("test-official-scheduler-");

        SchedulingConfiguration configuration = new SchedulingConfiguration(officialProperties);
        ThreadPoolTaskScheduler scheduler = configuration.officialTaskScheduler();
        scheduler.initialize();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        try {
            assertThat(scheduler.getScheduledThreadPoolExecutor().getCorePoolSize()).isEqualTo(1);
            assertThat(scheduler.getScheduledThreadPoolExecutor().getRemoveOnCancelPolicy()).isTrue();
            assertThat(ReflectionTestUtils.getField(scheduler, "waitForTasksToCompleteOnShutdown")).isEqualTo(true);
            assertThat(ReflectionTestUtils.getField(scheduler, "awaitTerminationMillis")).isEqualTo(15000L);

            scheduler.schedule(countDownLatch::countDown, Instant.now().plusMillis(50));
            assertThat(countDownLatch.await(2, TimeUnit.SECONDS)).isTrue();
        } finally {
            scheduler.shutdown();
        }
    }
}
