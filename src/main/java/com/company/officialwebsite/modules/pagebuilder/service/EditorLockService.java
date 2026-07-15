package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.common.dto.LockAcquireDTO;
import com.company.officialwebsite.common.dto.LockForceReleaseDTO;
import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.vo.LockStatusVO;

import java.util.Map;

/**
 * EditorLockService：独占编辑锁核心业务服务接口。
 */
public interface EditorLockService {

    /**
     * 心跳维持间隔时间 (秒)
     */
    int HEARTBEAT_INTERVAL_SECONDS = 30;

    /**
     * 锁默认超时时间 (分钟)
     */
    int LOCK_EXPIRE_MINUTES = 5;

    /**
     * 查询指定资源的当前锁状态。
     *
     * @param canForceUnlock 当前操作人是否拥有强制解锁权限，影响响应中的 forceUnlockAllowed 字段
     */
    LockStatusVO getLockStatus(EditorResourceTypeEnum resourceType, Long resourceId, String currentUsername, boolean canForceUnlock);

    /**
     * 申请获取独占编辑锁。
     *
     * @param canForceUnlock 当前操作人是否拥有强制解锁权限，影响响应中的 forceUnlockAllowed 字段
     */
    LockStatusVO acquireLock(EditorResourceTypeEnum resourceType, Long resourceId, LockAcquireDTO dto, String currentUsername, String currentDisplayName, boolean canForceUnlock);

    /**
     * 锁心跳续期。
     */
    LockStatusVO heartbeat(EditorResourceTypeEnum resourceType, Long resourceId, String lockToken, String currentUsername);

    /**
     * 主动释放独占编辑锁。
     */
    void releaseLock(EditorResourceTypeEnum resourceType, Long resourceId, String lockToken, String currentUsername);

    /**
     * 强制释放独占编辑锁（高权限审计操作）。
     *
     * @param canForceUnlock 当前操作人是否拥有强制解锁权限，若为 false 抛 EDITOR_LOCK_FORCE_RELEASE_DENIED(20004)
     */
    Map<String, Object> forceRelease(EditorResourceTypeEnum resourceType, Long resourceId, LockForceReleaseDTO dto, String currentUsername, boolean canForceUnlock);

    /**
     * 校验当前请求者是否持有有效独占锁（供后续草稿、预览、发布、回滚、下线接口调用）。
     */
    void validateLock(EditorResourceTypeEnum resourceType, Long resourceId, String lockToken, String currentUsername);
}
