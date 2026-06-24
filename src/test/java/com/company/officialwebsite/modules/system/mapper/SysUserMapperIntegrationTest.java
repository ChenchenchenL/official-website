package com.company.officialwebsite.modules.system.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.exceptions.MybatisPlusException;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.enums.RoleCode;
import com.company.officialwebsite.common.enums.UserStatus;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.system.entity.SysUserEntity;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.MyBatisSystemException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

/**
 * SysUserMapperIntegrationTest：验证 MyBatis-Plus 自动填充、逻辑删除、乐观锁和分页拦截规则。
 */
@SpringBootTest
@Transactional
class SysUserMapperIntegrationTest {

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private DataSource dataSource;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void insert_shouldAutoFillAuditFields_whenOperatorExists() {
        authenticateAs(9001L);
        SysUserEntity entity = buildUser("autofill-operator");

        int rows = sysUserMapper.insert(entity);

        assertEquals(1, rows);
        assertNotNull(entity.getId());
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(0, entity.getVersion());
        assertEquals(0L, entity.getDeletedMarker());
        assertEquals(9001L, entity.getCreatedBy());
        assertEquals(9001L, entity.getUpdatedBy());
    }

    @Test
    void insert_shouldAutoFillDefaultFields_whenOperatorMissing() {
        SysUserEntity entity = buildUser("autofill-anonymous");

        int rows = sysUserMapper.insert(entity);

        assertEquals(1, rows);
        assertNotNull(entity.getCreatedAt());
        assertNotNull(entity.getUpdatedAt());
        assertEquals(0, entity.getVersion());
        assertEquals(0L, entity.getDeletedMarker());
        assertNull(entity.getCreatedBy());
        assertNull(entity.getUpdatedBy());
    }

    @Test
    void updateById_shouldUseOptimisticLockAndRefreshAuditFields() {
        authenticateAs(9002L);
        SysUserEntity entity = buildUser("optimistic-lock");
        sysUserMapper.insert(entity);

        SysUserEntity currentSnapshot = sysUserMapper.selectById(entity.getId());
        LocalDateTime originalUpdatedAt = currentSnapshot.getUpdatedAt();

        authenticateAs(9003L);
        currentSnapshot.setDisplayName("第一次更新");
        int firstUpdateRows = sysUserMapper.updateById(currentSnapshot);

        SysUserEntity staleSnapshot = new SysUserEntity();
        staleSnapshot.setId(entity.getId());
        staleSnapshot.setVersion(0);
        staleSnapshot.setDisplayName("过期更新");
        int staleUpdateRows = sysUserMapper.updateById(staleSnapshot);

        SysUserEntity refreshed = sysUserMapper.selectById(entity.getId());
        assertEquals(1, firstUpdateRows);
        assertEquals(0, staleUpdateRows);
        assertEquals(1, refreshed.getVersion());
        assertEquals(9003L, refreshed.getUpdatedBy());
        assertTrue(!refreshed.getUpdatedAt().isBefore(originalUpdatedAt));
    }

    @Test
    void deleteById_shouldSoftDeleteRecordAndHideItFromDefaultQuery() throws SQLException {
        authenticateAs(9004L);
        SysUserEntity entity = buildUser("logic-delete");
        sysUserMapper.insert(entity);

        int deleteRows = sysUserMapper.deleteById(entity.getId());
        SysUserEntity deletedEntity = sysUserMapper.selectById(entity.getId());
        Long deletedMarker = queryDeletedMarker(entity.getId());

        assertEquals(1, deleteRows);
        assertNull(deletedEntity);
        assertEquals(entity.getId(), deletedMarker);
    }

    @Test
    void selectPage_shouldApplyPaginationLimit() {
        authenticateAs(9005L);
        String scenario = "page-limit";
        for (int index = 0; index < 105; index++) {
            sysUserMapper.insert(buildUser(scenario + "-" + index));
        }

        Page<SysUserEntity> page = sysUserMapper.selectPage(
                new Page<>(1, 200),
                new LambdaQueryWrapper<SysUserEntity>()
                        .like(SysUserEntity::getUsername, scenario)
                        .orderByAsc(SysUserEntity::getId));

        assertEquals(105, page.getTotal());
        assertEquals(100, page.getRecords().size());
    }

    @Test
    void delete_shouldRejectWhenWrapperHasNoBusinessCondition() {
        authenticateAs(9006L);
        sysUserMapper.insert(buildUser("block-attack"));

        MyBatisSystemException exception = assertThrows(
                MyBatisSystemException.class,
                () -> sysUserMapper.delete(new LambdaQueryWrapper<>()));

        Throwable mybatisPlusCause = findCause(exception, MybatisPlusException.class);
        assertNotNull(mybatisPlusCause);
        assertInstanceOf(MybatisPlusException.class, mybatisPlusCause);
        assertTrue(mybatisPlusCause.getMessage().contains("Prohibition of table update operation"));
    }

    private SysUserEntity buildUser(String scenario) {
        String uniqueToken = scenario + "-" + UUID.randomUUID();
        SysUserEntity entity = new SysUserEntity();
        entity.setUsername(uniqueToken);
        entity.setPasswordHash("$2a$10$FIrWp7hmOOezCwXWiaurve0KNTc6pRBSWjIAff0HgXOTUfDxuX9NK");
        entity.setDisplayName("测试用户-" + scenario);
        entity.setRoleCode(RoleCode.CONTENT_EDITOR.getCode());
        entity.setStatus(UserStatus.ENABLED.getCode());
        entity.setLastLoginAt(null);
        return entity;
    }

    /**
     * 集成测试通过事务内连接读取原始行，确认逻辑删除后的真实数据库值。
     */
    private Long queryDeletedMarker(Long userId) throws SQLException {
        Connection connection = DataSourceUtils.getConnection(dataSource);
        try (PreparedStatement preparedStatement =
                connection.prepareStatement("SELECT deleted_marker FROM sys_user WHERE id = ?")) {
            preparedStatement.setLong(1, userId);
            try (ResultSet resultSet = preparedStatement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getLong("deleted_marker");
                }
                return null;
            }
        }
    }

    /**
     * MyBatis-Spring 会按场景包裹异常，这里沿异常链查找真实业务根因。
     */
    private Throwable findCause(Throwable throwable, Class<? extends Throwable> targetType) {
        Throwable current = throwable;
        while (current != null) {
            if (targetType.isInstance(current)) {
                return current;
            }
            current = current.getCause();
        }
        return null;
    }

    private void authenticateAs(Long userId) {
        AdminUserPrincipal principal = new AdminUserPrincipal(
                userId,
                "tester-" + userId,
                "encoded-password",
                "测试操作者",
                RoleCode.ADMINISTRATOR.getCode(),
                UserStatus.ENABLED.getCode());
        UsernamePasswordAuthenticationToken authenticationToken = UsernamePasswordAuthenticationToken.authenticated(
                principal,
                principal.getPassword(),
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
