package com.company.officialwebsite.common.vo;

import java.time.LocalDateTime;

/**
 * LockStatusVO：统一编辑器独占锁响应对象。
 * <p>
 * 遵循缺口清单 4.1 统一锁响应：
 * - 仅在持锁成功时返回 {@code lockToken}；查询锁状态与锁冲突时不返回明文 {@code lockToken}。
 * </p>
 */
public class LockStatusVO {

    private String resourceType;
    private Long resourceId;
    private Boolean editable;
    private String lockToken;
    private String ownerDisplayName;
    private LocalDateTime acquiredAt;
    private LocalDateTime expiresAt;
    private Integer heartbeatIntervalSeconds;
    private Boolean forceUnlockAllowed;

    public LockStatusVO() {
    }

    public LockStatusVO(
            String resourceType,
            Long resourceId,
            Boolean editable,
            String lockToken,
            String ownerDisplayName,
            LocalDateTime acquiredAt,
            LocalDateTime expiresAt,
            Integer heartbeatIntervalSeconds,
            Boolean forceUnlockAllowed) {
        this.resourceType = resourceType;
        this.resourceId = resourceId;
        this.editable = editable;
        this.lockToken = lockToken;
        this.ownerDisplayName = ownerDisplayName;
        this.acquiredAt = acquiredAt;
        this.expiresAt = expiresAt;
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
        this.forceUnlockAllowed = forceUnlockAllowed;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public Long getResourceId() {
        return resourceId;
    }

    public void setResourceId(Long resourceId) {
        this.resourceId = resourceId;
    }

    public Boolean getEditable() {
        return editable;
    }

    public boolean isEditable() {
        return Boolean.TRUE.equals(editable);
    }

    public void setEditable(Boolean editable) {
        this.editable = editable;
    }

    public String getLockToken() {
        return lockToken;
    }

    public void setLockToken(String lockToken) {
        this.lockToken = lockToken;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public LocalDateTime getAcquiredAt() {
        return acquiredAt;
    }

    public void setAcquiredAt(LocalDateTime acquiredAt) {
        this.acquiredAt = acquiredAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getHeartbeatIntervalSeconds() {
        return heartbeatIntervalSeconds;
    }

    public void setHeartbeatIntervalSeconds(Integer heartbeatIntervalSeconds) {
        this.heartbeatIntervalSeconds = heartbeatIntervalSeconds;
    }

    public Boolean getForceUnlockAllowed() {
        return forceUnlockAllowed;
    }

    public void setForceUnlockAllowed(Boolean forceUnlockAllowed) {
        this.forceUnlockAllowed = forceUnlockAllowed;
    }
}
