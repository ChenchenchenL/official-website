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

    private final Storage storage = new Storage();

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

    public Storage getStorage() {
        return storage;
    }

    public static class Cache {

        private String keyPrefix = "official";

        private Duration defaultTtl = Duration.ofMinutes(10);

        private Duration secondDeleteDelay = Duration.ofSeconds(1);

        /** 排序间隔步长，默认 10 */
        private int sortGap = 10;

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

        public int getSortGap() {
            return sortGap;
        }

        public void setSortGap(int sortGap) {
            this.sortGap = sortGap;
        }
    }

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

    public static class Json {

        private boolean writeLongAsString = false;

        public boolean isWriteLongAsString() {
            return writeLongAsString;
        }

        public void setWriteLongAsString(boolean writeLongAsString) {
            this.writeLongAsString = writeLongAsString;
        }
    }

    public static class Storage {

        private String localRootDir = System.getProperty("java.io.tmpdir") + "/official-website/media";

        private String publicUrlPrefix = "/media/public/";

        private long maxImageSizeBytes = 2 * 1024 * 1024L;

        public String getLocalRootDir() {
            return localRootDir;
        }

        public void setLocalRootDir(String localRootDir) {
            this.localRootDir = localRootDir;
        }

        public String getPublicUrlPrefix() {
            return publicUrlPrefix;
        }

        public void setPublicUrlPrefix(String publicUrlPrefix) {
            this.publicUrlPrefix = publicUrlPrefix;
        }

        public long getMaxImageSizeBytes() {
            return maxImageSizeBytes;
        }

        public void setMaxImageSizeBytes(long maxImageSizeBytes) {
            this.maxImageSizeBytes = maxImageSizeBytes;
        }
    }
}
