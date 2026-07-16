package com.company.officialwebsite.infrastructure.cache;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;
import java.time.LocalDateTime;

/** 持久化记录 Portal 缓存删除失败后的补偿任务。 */
@TableName("portal_cache_invalidation_retry")
public class PortalCacheInvalidationRetryEntity extends BaseEntity {

    private String cacheKeys;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private String lastError;
    private String status;

    public String getCacheKeys() { return cacheKeys; }
    public void setCacheKeys(String cacheKeys) { this.cacheKeys = cacheKeys; }
    public Integer getRetryCount() { return retryCount; }
    public void setRetryCount(Integer retryCount) { this.retryCount = retryCount; }
    public LocalDateTime getNextRetryAt() { return nextRetryAt; }
    public void setNextRetryAt(LocalDateTime nextRetryAt) { this.nextRetryAt = nextRetryAt; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
