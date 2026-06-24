package com.company.officialwebsite.common.config;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * SchedulingConfiguration：统一配置应用内调度线程池，供延迟二删和周期性任务复用。
 */
@Configuration
@EnableScheduling
public class SchedulingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(SchedulingConfiguration.class);

    private final OfficialProperties officialProperties;

    public SchedulingConfiguration(OfficialProperties officialProperties) {
        this.officialProperties = officialProperties;
    }

    /**
     * 所有调度任务共用一个线程池，避免各模块自行创建定时器导致关闭和监控失控。
     */
    @Bean(name = {"officialTaskScheduler", "taskScheduler"})
    @Primary
    public ThreadPoolTaskScheduler officialTaskScheduler() {
        OfficialProperties.Scheduler schedulerProperties = officialProperties.getScheduler();
        ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(schedulerProperties.getPoolSize());
        taskScheduler.setThreadNamePrefix(schedulerProperties.getThreadNamePrefix());
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(toSeconds(schedulerProperties.getAwaitTermination()));
        taskScheduler.setRemoveOnCancelPolicy(true);
        taskScheduler.setErrorHandler(ex -> log.error(
                "scheduled task failed exception={} message={}",
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex));
        return taskScheduler;
    }

    private int toSeconds(Duration duration) {
        return Math.toIntExact(duration.getSeconds());
    }
}
