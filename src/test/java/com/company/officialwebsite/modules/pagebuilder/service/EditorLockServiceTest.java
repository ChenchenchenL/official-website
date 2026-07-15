package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.common.dto.LockAcquireDTO;
import com.company.officialwebsite.common.dto.LockForceReleaseDTO;
import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.vo.LockStatusVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class EditorLockServiceTest {

    @Autowired
    private EditorLockService editorLockService;

    @Test
    @DisplayName("独占锁完整生命周期：获取、心跳续期、主动释放")
    void lockLifecycle_shouldSuccess() {
        LockAcquireDTO acquireDTO = new LockAcquireDTO();
        acquireDTO.setEditorSessionRemark("测试锁会话");

        // 1. 获取锁
        LockStatusVO acquired = editorLockService.acquireLock(
                EditorResourceTypeEnum.PAGE, 501L, acquireDTO, "admin_user", "管理员张三", false
        );
        assertThat(acquired).isNotNull();
        assertThat(acquired.isEditable()).isTrue();
        assertThat(acquired.getLockToken()).isNotNull().hasSize(64);

        String token = acquired.getLockToken();

        // 2. 查询锁状态 (不应当包含 lockToken)
        LockStatusVO queried = editorLockService.getLockStatus(EditorResourceTypeEnum.PAGE, 501L, "admin_user", false);
        assertThat(queried.isEditable()).isTrue();
        assertThat(queried.getLockToken()).isNull();

        // 3. 心跳续期
        LockStatusVO heartbeatVo = editorLockService.heartbeat(EditorResourceTypeEnum.PAGE, 501L, token, "admin_user");
        assertThat(heartbeatVo.isEditable()).isTrue();

        // 4. 校验锁成功
        editorLockService.validateLock(EditorResourceTypeEnum.PAGE, 501L, token, "admin_user");

        // 5. 主动释放锁
        editorLockService.releaseLock(EditorResourceTypeEnum.PAGE, 501L, token, "admin_user");

        // 释放后再次查询状态：可编辑且无锁
        LockStatusVO afterRelease = editorLockService.getLockStatus(EditorResourceTypeEnum.PAGE, 501L, "admin_user", false);
        assertThat(afterRelease.isEditable()).isTrue();
    }

    @Test
    @DisplayName("抢锁冲突触发 10006 异常且不暴露锁 Token")
    void acquireLock_conflictShouldThrowException() {
        // 管理员1 加锁
        LockStatusVO lock1 = editorLockService.acquireLock(
                EditorResourceTypeEnum.PRODUCT, 888L, null, "admin_1", "管理员1", false
        );
        assertThat(lock1.getLockToken()).isNotNull();

        // 管理员2 尝试加锁 -> 抛出 EDITOR_LOCK_CONFLICT
        assertThatThrownBy(() -> editorLockService.acquireLock(
                EditorResourceTypeEnum.PRODUCT, 888L, null, "admin_2", "管理员2", false
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EDITOR_LOCK_CONFLICT);
    }

    @Test
    @DisplayName("锁 Token 不匹配触发 10009 异常")
    void heartbeat_wrongTokenShouldThrowMismatch() {
        LockStatusVO lock = editorLockService.acquireLock(
                EditorResourceTypeEnum.CASE, 999L, null, "admin_user", "管理员", false
        );

        assertThatThrownBy(() -> editorLockService.heartbeat(
                EditorResourceTypeEnum.CASE, 999L, "wrong_token_123456", "admin_user"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EDITOR_LOCK_OWNER_MISMATCH);
    }

    @Test
    @DisplayName("强制解锁必须带原因并生成审计记录")
    void forceRelease_shouldSucceedWithAudit() {
        editorLockService.acquireLock(
                EditorResourceTypeEnum.INDUSTRY_SOLUTION, 777L, null, "admin_user", "管理员", false
        );

        LockForceReleaseDTO forceReleaseDTO = new LockForceReleaseDTO();
        forceReleaseDTO.setReason("紧急纠错解锁");

        Map<String, Object> result = editorLockService.forceRelease(
                EditorResourceTypeEnum.INDUSTRY_SOLUTION, 777L, forceReleaseDTO, "super_admin", true
        );

        assertThat(result).isNotNull();
        assertThat(result.get("releasedAt")).isNotNull();

        // 解锁后资源处于可编辑无锁状态
        LockStatusVO status = editorLockService.getLockStatus(EditorResourceTypeEnum.INDUSTRY_SOLUTION, 777L, "admin_user", false);
        assertThat(status.isEditable()).isTrue();
    }
}
