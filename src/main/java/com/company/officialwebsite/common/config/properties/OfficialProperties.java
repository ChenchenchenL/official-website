package com.company.officialwebsite.common.config.properties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OfficialProperties：统一承载项目级自定义配置，避免业务代码分散读取零散配置项。
 */
@ConfigurationProperties(prefix = "official")
public class OfficialProperties {

    private final Cache cache = new Cache();

    private final Cors cors = new Cors();

    private final Async async = new Async();

    private final Scheduler scheduler = new Scheduler();

    private final Security security = new Security();

    private final Json json = new Json();

    public Cache getCache() {
        return cache;
    }

    public Cors getCors() {
        return cors;
    }

    public Async getAsync() {
        return async;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public Security getSecurity() {
        return security;
    }

    public Json getJson() {
        return json;
    }

    /**
     * Cache：统一约束 Portal 与通用缓存的前缀和过期时间默认值。
     */
    public static class Cache {

        private String keyPrefix = "official";

        private Duration defaultTtl = Duration.ofMinutes(10);

        private Duration secondDeleteDelay = Duration.ofSeconds(1);

        public String getKeyPrefix() {
            return keyPrefix;
        }

        public void setKeyPrefix(String keyPrefix) {
            this.keyPrefix = keyPrefix;
        }

        public Duration getDefaultTtl() {
            return defaultTtl;
        }

        public void setDefaultTtl(Duration defaultTtl) {
            this.defaultTtl = defaultTtl;
        }

        public Duration getSecondDeleteDelay() {
            return secondDeleteDelay;
        }

        public void setSecondDeleteDelay(Duration secondDeleteDelay) {
            this.secondDeleteDelay = secondDeleteDelay;
        }
    }

    /**
     * Cors：分别管理 Admin 与 Portal 的跨域策略，避免公开接口与管理端共用错误白名单。
     */
    public static class Cors {

        private final Scope admin = new Scope();

        private final Scope portal = new Scope();

        public Scope getAdmin() {
            return admin;
        }

        public Scope getPortal() {
            return portal;
        }
    }

    /**
     * Scope：声明单个跨域作用域下的允许来源与预检缓存时间。
     */
    public static class Scope {

        private List<String> allowedOrigins = new ArrayList<>();

        private List<String> allowedOriginPatterns = new ArrayList<>();

        private Duration maxAge = Duration.ofHours(1);

        public List<String> getAllowedOrigins() {
            return allowedOrigins;
        }

        public void setAllowedOrigins(List<String> allowedOrigins) {
            this.allowedOrigins = allowedOrigins;
        }

        public List<String> getAllowedOriginPatterns() {
            return allowedOriginPatterns;
        }

        public void setAllowedOriginPatterns(List<String> allowedOriginPatterns) {
            this.allowedOriginPatterns = allowedOriginPatterns;
        }

        public Duration getMaxAge() {
            return maxAge;
        }

        public void setMaxAge(Duration maxAge) {
            this.maxAge = maxAge;
        }
    }

    /**
     * Async：统一管理应用内异步任务执行器的线程池参数。
     */
    public static class Async {

        private int corePoolSize = 2;

        private int maxPoolSize = 8;

        private int queueCapacity = 200;

        private Duration keepAlive = Duration.ofSeconds(60);

        private Duration awaitTermination = Duration.ofSeconds(30);

        private String threadNamePrefix = "official-async-";

        public int getCorePoolSize() {
            return corePoolSize;
        }

        public void setCorePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
        }

        public int getMaxPoolSize() {
            return maxPoolSize;
        }

        public void setMaxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }

        public Duration getKeepAlive() {
            return keepAlive;
        }

        public void setKeepAlive(Duration keepAlive) {
            this.keepAlive = keepAlive;
        }

        public Duration getAwaitTermination() {
            return awaitTermination;
        }

        public void setAwaitTermination(Duration awaitTermination) {
            this.awaitTermination = awaitTermination;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }

    /**
     * Scheduler：统一管理应用内调度线程池，供延迟二删和周期任务复用。
     */
    public static class Scheduler {

        private int poolSize = 2;

        private Duration awaitTermination = Duration.ofSeconds(30);

        private String threadNamePrefix = "official-scheduler-";

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }

        public Duration getAwaitTermination() {
            return awaitTermination;
        }

        public void setAwaitTermination(Duration awaitTermination) {
            this.awaitTermination = awaitTermination;
        }

        public String getThreadNamePrefix() {
            return threadNamePrefix;
        }

        public void setThreadNamePrefix(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }
    }

    /**
     * Security：统一管理 Cookie Session 场景下的安全相关基础配置。
     */
    public static class Security {

        private String csrfHeaderName = "X-XSRF-TOKEN";

        private String csrfCookieName = "XSRF-TOKEN";

        private String csrfCookiePath = "/";

        private String csrfCookieSameSite = "Lax";

        public String getCsrfHeaderName() {
            return csrfHeaderName;
        }

        public void setCsrfHeaderName(String csrfHeaderName) {
            this.csrfHeaderName = csrfHeaderName;
        }

        public String getCsrfCookieName() {
            return csrfCookieName;
        }

        public void setCsrfCookieName(String csrfCookieName) {
            this.csrfCookieName = csrfCookieName;
        }

        public String getCsrfCookiePath() {
            return csrfCookiePath;
        }

        public void setCsrfCookiePath(String csrfCookiePath) {
            this.csrfCookiePath = csrfCookiePath;
        }

        public String getCsrfCookieSameSite() {
            return csrfCookieSameSite;
        }

        public void setCsrfCookieSameSite(String csrfCookieSameSite) {
            this.csrfCookieSameSite = csrfCookieSameSite;
        }
    }

    /**
     * Json：统一管理接口序列化风格，避免各模块各自改写 ObjectMapper。
     */
    public static class Json {

        private boolean writeLongAsString = false;

        public boolean isWriteLongAsString() {
            return writeLongAsString;
        }

        public void setWriteLongAsString(boolean writeLongAsString) {
            this.writeLongAsString = writeLongAsString;
        }
    }
}
