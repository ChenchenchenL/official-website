package com.company.officialwebsite.modules.pagebuilder.mapper;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.modules.pagebuilder.entity.EditorLockEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class EditorLockMapperTest {

    @Autowired
    private EditorLockMapper editorLockMapper;

    @Test
    @DisplayName("插入与查询独占锁记录正常")
    void insertAndSelectLock_shouldSuccess() {
        EditorLockEntity lock = new EditorLockEntity();
        lock.setResourceType(EditorResourceTypeEnum.PAGE.name());
        lock.setResourceId(1001L);
        lock.setLockedBy("admin_user");
        lock.setOwnerDisplayName("管理员");
        lock.setLockTokenHash("dummy_hash_12345");
        lock.setAcquiredAt(LocalDateTime.now());
        lock.setLastHeartbeatAt(LocalDateTime.now());
        lock.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        lock.setEditorSessionRemark("测试锁创建");

        int rows = editorLockMapper.insert(lock);
        assertThat(rows).isEqualTo(1);
        assertThat(lock.getId()).isNotNull();

        EditorLockEntity fetched = editorLockMapper.selectById(lock.getId());
        assertThat(fetched).isNotNull();
        assertThat(fetched.getResourceType()).isEqualTo(EditorResourceTypeEnum.PAGE.name());
        assertThat(fetched.getLockedBy()).isEqualTo("admin_user");
    }

    @Test
    @DisplayName("相同资源重复加锁触发数据库联合唯一索引阻断")
    void insertDuplicateLock_shouldTriggerDuplicateKeyException() {
        EditorLockEntity lock1 = new EditorLockEntity();
        lock1.setResourceType(EditorResourceTypeEnum.PRODUCT.name());
        lock1.setResourceId(888L);
        lock1.setLockedBy("user_1");
        lock1.setOwnerDisplayName("管理员1");
        lock1.setLockTokenHash("hash_111");
        lock1.setAcquiredAt(LocalDateTime.now());
        lock1.setLastHeartbeatAt(LocalDateTime.now());
        lock1.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        editorLockMapper.insert(lock1);

        EditorLockEntity lock2 = new EditorLockEntity();
        lock2.setResourceType(EditorResourceTypeEnum.PRODUCT.name());
        lock2.setResourceId(888L);
        lock2.setLockedBy("user_2");
        lock2.setOwnerDisplayName("管理员2");
        lock2.setLockTokenHash("hash_222");
        lock2.setAcquiredAt(LocalDateTime.now());
        lock2.setLastHeartbeatAt(LocalDateTime.now());
        lock2.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        assertThatThrownBy(() -> editorLockMapper.insert(lock2))
                .isInstanceOf(DuplicateKeyException.class);
    }
}
