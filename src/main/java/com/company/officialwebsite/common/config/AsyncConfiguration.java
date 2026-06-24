package com.company.officialwebsite.common.config;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.trace.TraceContext;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * AsyncConfiguration：统一配置应用内异步执行器，并透传日志追踪与认证上下文。
 */
@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfiguration.class);

    private final OfficialProperties officialProperties;
    private final ApplicationContext applicationContext;

    public AsyncConfiguration(OfficialProperties officialProperties, ApplicationContext applicationContext) {
        this.officialProperties = officialProperties;
        this.applicationContext = applicationContext;
    }

    /**
     * 提供统一异步线程池，供缓存二次删除、媒体处理和异步审计等任务复用。
     */
    @Bean(name = "officialAsyncExecutor")
    @Primary
    public ThreadPoolTaskExecutor officialAsyncExecutor() {
        OfficialProperties.Async asyncProperties = officialProperties.getAsync();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(asyncProperties.getCorePoolSize());
        executor.setMaxPoolSize(asyncProperties.getMaxPoolSize());
        executor.setQueueCapacity(asyncProperties.getQueueCapacity());
        executor.setKeepAliveSeconds(toSeconds(asyncProperties.getKeepAlive()));
        executor.setThreadNamePrefix(asyncProperties.getThreadNamePrefix());
        executor.setTaskDecorator(officialAsyncTaskDecorator());
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(toSeconds(asyncProperties.getAwaitTermination()));
        return executor;
    }

    /**
     * 在线程切换时复制 traceId 和认证主体，避免异步日志、审计任务丢失链路信息。
     */
    @Bean
    public TaskDecorator officialAsyncTaskDecorator() {
        return runnable -> {
            Map<String, String> callerMdcContext = MDC.getCopyOfContextMap();
            SecurityContext callerSecurityContext = copySecurityContext(SecurityContextHolder.getContext());
            return () -> {
                Map<String, String> originalMdcContext = MDC.getCopyOfContextMap();
                SecurityContext originalSecurityContext = SecurityContextHolder.getContext();
                try {
                    applyMdcContext(callerMdcContext);
                    SecurityContextHolder.setContext(copySecurityContext(callerSecurityContext));
                    runnable.run();
                } finally {
                    applyMdcContext(originalMdcContext);
                    SecurityContextHolder.setContext(originalSecurityContext);
                }
            };
        };
    }

    /**
     * 统一承接 void 异步方法未捕获异常，避免静默失败。
     */
    @Bean
    public AsyncUncaughtExceptionHandler officialAsyncUncaughtExceptionHandler() {
        return new OfficialAsyncUncaughtExceptionHandler();
    }

    @Override
    public Executor getAsyncExecutor() {
        return applicationContext.getBean("officialAsyncExecutor", Executor.class);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return applicationContext.getBean(AsyncUncaughtExceptionHandler.class);
    }

    private int toSeconds(Duration duration) {
        return Math.toIntExact(duration.getSeconds());
    }

    private SecurityContext copySecurityContext(SecurityContext sourceContext) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        if (sourceContext != null) {
            Authentication authentication = sourceContext.getAuthentication();
            if (authentication != null) {
                securityContext.setAuthentication(authentication);
            }
        }
        return securityContext;
    }

    private void applyMdcContext(Map<String, String> mdcContext) {
        if (mdcContext == null || mdcContext.isEmpty()) {
            MDC.clear();
            return;
        }
        MDC.setContextMap(mdcContext);
    }

    /**
     * OfficialAsyncUncaughtExceptionHandler：统一记录异步方法的未捕获异常。
     */
    private static final class OfficialAsyncUncaughtExceptionHandler implements AsyncUncaughtExceptionHandler {

        @Override
        public void handleUncaughtException(Throwable ex, Method method, Object... params) {
            log.error("async task failed method={} exception={} traceId={}",
                    method.getName(), ex.getClass().getSimpleName(), TraceContext.getTraceId(), ex);
        }
    }
}
