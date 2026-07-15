package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.dto.LockAcquireDTO;
import com.company.officialwebsite.common.dto.LockForceReleaseDTO;
import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.DataMaskUtils;
import com.company.officialwebsite.common.utils.LockTokenUtils;

import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.modules.pagebuilder.entity.EditorLockEntity;
import com.company.officialwebsite.modules.pagebuilder.mapper.EditorLockMapper;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * EditorLockServiceImpl：独占编辑锁核心业务服务实现。
 */
@Service
public class EditorLockServiceImpl implements EditorLockService {

    private static final Logger log = LoggerFactory.getLogger(EditorLockServiceImpl.class);

    private static final String BIZ_MODULE = "EDITOR";
    private static final String TARGET_TYPE = "EDITOR_LOCK";
    private static final String ACTION_FORCE_RELEASE = "FORCE_RELEASE_EDITOR_LOCK";

    private final EditorLockMapper editorLockMapper;
    private final AuditLogService auditLogService;

    public EditorLockServiceImpl(EditorLockMapper editorLockMapper, AuditLogService auditLogService) {
        this.editorLockMapper = editorLockMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public LockStatusVO getLockStatus(EditorResourceTypeEnum resourceType, Long resourceId, String currentUsername, boolean canForceUnlock) {
        validateResourceParams(resourceType, resourceId);
        EditorLockEntity activeLock = findActiveLock(resourceType, resourceId);
        if (activeLock == null || isExpired(activeLock)) {
            return new LockStatusVO(resourceType.name(), resourceId, true, null, null, null, null, HEARTBEAT_INTERVAL_SECONDS, canForceUnlock);
        }
        boolean isOwner = activeLock.getLockedBy().equalsIgnoreCase(currentUsername);
        return new LockStatusVO(
                resourceType.name(),
                resourceId,
                isOwner,
                null, // 查询锁状态不返回 lockToken
                DataMaskUtils.maskUsername(activeLock.getOwnerDisplayName()),
                activeLock.getAcquiredAt(),
                activeLock.getExpiresAt(),
                HEARTBEAT_INTERVAL_SECONDS,
                canForceUnlock
        );
    }

    @Override
    @Transactional
    public LockStatusVO acquireLock(EditorResourceTypeEnum resourceType, Long resourceId, LockAcquireDTO dto, String currentUsername, String currentDisplayName, boolean canForceUnlock) {
        validateResourceParams(resourceType, resourceId);
        if (!StringUtils.hasText(currentUsername)) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        EditorLockEntity activeLock = findActiveLock(resourceType, resourceId);
        if (activeLock != null) {
            if (isExpired(activeLock)) {
                log.info("existing editor lock expired id={} resourceType={} resourceId={}, cleaning for new lock",
                        activeLock.getId(), resourceType, resourceId);
                editorLockMapper.deleteById(activeLock.getId());
            } else if (activeLock.getLockedBy().equalsIgnoreCase(currentUsername)) {
                // 当前管理员重新获取锁：刷新心跳并生成新 Token
                String newToken = LockTokenUtils.generateToken();
                activeLock.setLockTokenHash(LockTokenUtils.hashToken(newToken));
                activeLock.setLastHeartbeatAt(LocalDateTime.now());
                activeLock.setExpiresAt(LocalDateTime.now().plusMinutes(LOCK_EXPIRE_MINUTES));
                if (dto != null && StringUtils.hasText(dto.getEditorSessionRemark())) {
                    activeLock.setEditorSessionRemark(dto.getEditorSessionRemark().trim());
                }
                editorLockMapper.updateById(activeLock);

                log.info("refresh acquired editor lock resourceType={} resourceId={} username={} token={}",
                        resourceType, resourceId, currentUsername, LockTokenUtils.maskToken(newToken));
                return new LockStatusVO(
                        resourceType.name(), resourceId, true, newToken,
                        currentDisplayName, activeLock.getAcquiredAt(), activeLock.getExpiresAt(),
                        HEARTBEAT_INTERVAL_SECONDS, canForceUnlock);
            } else {
                log.warn("acquire editor lock conflict resourceType={} resourceId={} lockedBy={}",
                        resourceType, resourceId, activeLock.getLockedBy());
                throw new BusinessException(
                        ErrorCode.EDITOR_LOCK_CONFLICT,
                        "该资源已被管理员 " + DataMaskUtils.maskUsername(activeLock.getOwnerDisplayName()) + " 锁定"
                );
            }
        }

        String rawToken = LockTokenUtils.generateToken();
        EditorLockEntity newLock = new EditorLockEntity();
        newLock.setResourceType(resourceType.name());
        newLock.setResourceId(resourceId);
        newLock.setLockedBy(currentUsername);
        newLock.setOwnerDisplayName(StringUtils.hasText(currentDisplayName) ? currentDisplayName : currentUsername);
        newLock.setLockTokenHash(LockTokenUtils.hashToken(rawToken));
        newLock.setAcquiredAt(LocalDateTime.now());
        newLock.setLastHeartbeatAt(LocalDateTime.now());
        newLock.setExpiresAt(LocalDateTime.now().plusMinutes(LOCK_EXPIRE_MINUTES));
        if (dto != null && StringUtils.hasText(dto.getEditorSessionRemark())) {
            newLock.setEditorSessionRemark(dto.getEditorSessionRemark().trim());
        }

        try {
            editorLockMapper.insert(newLock);
        } catch (DuplicateKeyException ex) {
            log.warn("concurrent acquire lock duplicate key resourceType={} resourceId={}", resourceType, resourceId);
            throw new BusinessException(ErrorCode.EDITOR_LOCK_CONFLICT, "该资源正在被其他管理员编辑");
        }

        log.info("acquire editor lock success resourceType={} resourceId={} username={} token={}",
                resourceType, resourceId, currentUsername, LockTokenUtils.maskToken(rawToken));

        return new LockStatusVO(
                resourceType.name(), resourceId, true, rawToken,
                newLock.getOwnerDisplayName(), newLock.getAcquiredAt(), newLock.getExpiresAt(),
                HEARTBEAT_INTERVAL_SECONDS, canForceUnlock);
    }

    @Override
    @Transactional
    public LockStatusVO heartbeat(EditorResourceTypeEnum resourceType, Long resourceId, String lockToken, String currentUsername) {
        EditorLockEntity lock = requireValidLock(resourceType, resourceId, lockToken, currentUsername);
        LocalDateTime now = LocalDateTime.now();
        lock.setLastHeartbeatAt(now);
        lock.setExpiresAt(now.plusMinutes(LOCK_EXPIRE_MINUTES));
        editorLockMapper.updateById(lock);

        log.info("heartbeat success resourceType={} resourceId={} username={} expiresAt={}",
                resourceType, resourceId, currentUsername, lock.getExpiresAt());

        return new LockStatusVO(
                resourceType.name(), resourceId, true, null,
                lock.getOwnerDisplayName(), lock.getAcquiredAt(), lock.getExpiresAt(),
                HEARTBEAT_INTERVAL_SECONDS, true);
    }

    @Override
    @Transactional
    public void releaseLock(EditorResourceTypeEnum resourceType, Long resourceId, String lockToken, String currentUsername) {
        EditorLockEntity lock = requireValidLock(resourceType, resourceId, lockToken, currentUsername);
        editorLockMapper.deleteById(lock.getId());
        log.info("release editor lock success resourceType={} resourceId={} username={}",
                resourceType, resourceId, currentUsername);
    }

    @Override
    @Transactional
    public Map<String, Object> forceRelease(EditorResourceTypeEnum resourceType, Long resourceId, LockForceReleaseDTO dto, String currentUsername, boolean canForceUnlock) {
        validateResourceParams(resourceType, resourceId);
        if (!canForceUnlock) {
            log.warn("force release denied user={} resourceType={} resourceId={}", currentUsername, resourceType, resourceId);
            throw new BusinessException(ErrorCode.EDITOR_LOCK_FORCE_RELEASE_DENIED);
        }
        if (dto == null || !StringUtils.hasText(dto.getReason())) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "强制解锁原因不能为空");
        }

        EditorLockEntity activeLock = findActiveLock(resourceType, resourceId);
        LocalDateTime now = LocalDateTime.now();

        if (activeLock != null) {
            Map<String, Object> beforeState = new HashMap<>();
            beforeState.put("id", activeLock.getId());
            beforeState.put("resourceType", activeLock.getResourceType());
            beforeState.put("resourceId", activeLock.getResourceId());
            beforeState.put("lockedBy", activeLock.getLockedBy());
            beforeState.put("ownerDisplayName", activeLock.getOwnerDisplayName());
            beforeState.put("expiresAt", activeLock.getExpiresAt());

            editorLockMapper.deleteById(activeLock.getId());

            Map<String, Object> afterState = new HashMap<>();
            afterState.put("released", true);
            afterState.put("releasedBy", currentUsername);
            afterState.put("reason", dto.getReason().trim());
            afterState.put("releasedAt", now);

            auditLogService.recordGenericOperation(
                    BIZ_MODULE, ACTION_FORCE_RELEASE, TARGET_TYPE, activeLock.getId(), beforeState, afterState);

            log.info("force release editor lock success resourceType={} resourceId={} releasedBy={}",
                    resourceType, resourceId, currentUsername);
        } else {
            // 无有效锁时也写审计，记录操作尝试
            Map<String, Object> noLockState = new HashMap<>();
            noLockState.put("resourceType", resourceType.name());
            noLockState.put("resourceId", resourceId);
            noLockState.put("releasedBy", currentUsername);
            noLockState.put("reason", dto.getReason().trim());
            noLockState.put("releasedAt", now);
            noLockState.put("released", false);
            noLockState.put("note", "no active lock found at the time of force release");

            auditLogService.recordGenericOperation(
                    BIZ_MODULE, ACTION_FORCE_RELEASE, TARGET_TYPE, resourceId, null, noLockState);

            log.info("force release attempted but no active lock found resourceType={} resourceId={} releasedBy={}",
                    resourceType, resourceId, currentUsername);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("releasedAt", now);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public void validateLock(EditorResourceTypeEnum resourceType, Long resourceId, String lockToken, String currentUsername) {
        requireValidLock(resourceType, resourceId, lockToken, currentUsername);
    }

    private EditorLockEntity requireValidLock(EditorResourceTypeEnum resourceType, Long resourceId, String lockToken, String currentUsername) {
        validateResourceParams(resourceType, resourceId);
        if (!StringUtils.hasText(lockToken)) {
            throw new BusinessException(ErrorCode.EDITOR_LOCK_TOKEN_REQUIRED);
        }
        EditorLockEntity activeLock = findActiveLock(resourceType, resourceId);
        if (activeLock == null) {
            throw new BusinessException(ErrorCode.EDITOR_LOCK_EXPIRED, "编辑锁已到期或已被释放");
        }
        if (isExpired(activeLock)) {
            throw new BusinessException(ErrorCode.EDITOR_LOCK_EXPIRED);
        }
        String inputHash = LockTokenUtils.hashToken(lockToken);
        if (!inputHash.equals(activeLock.getLockTokenHash()) || !activeLock.getLockedBy().equalsIgnoreCase(currentUsername)) {
            throw new BusinessException(ErrorCode.EDITOR_LOCK_OWNER_MISMATCH);
        }
        return activeLock;
    }

    private EditorLockEntity findActiveLock(EditorResourceTypeEnum resourceType, Long resourceId) {
        return editorLockMapper.selectOne(
                new LambdaQueryWrapper<EditorLockEntity>()
                        .eq(EditorLockEntity::getResourceType, resourceType.name())
                        .eq(EditorLockEntity::getResourceId, resourceId)
                        .eq(EditorLockEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
    }

    private boolean isExpired(EditorLockEntity lock) {
        return lock.getExpiresAt() != null && LocalDateTime.now().isAfter(lock.getExpiresAt());
    }

    private void validateResourceParams(EditorResourceTypeEnum resourceType, Long resourceId) {
        if (resourceType == null || resourceId == null || resourceId <= 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "资源类型或资源ID不合法");
        }
    }
}
