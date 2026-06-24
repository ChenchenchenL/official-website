package com.company.officialwebsite.common.config;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import java.time.LocalDateTime;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * MybatisPlusMetaObjectHandler：统一填充审计字段、版本号和逻辑删除初始值。
 */
@Component
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();
        Long operatorId = currentOperatorId();
        strictInsertFill(metaObject, "createdAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "updatedAt", LocalDateTime.class, now);
        strictInsertFill(metaObject, "version", Integer.class, 0);
        strictInsertFill(metaObject, "deletedMarker", Long.class, 0L);
        if (operatorId != null) {
            strictInsertFill(metaObject, "createdBy", Long.class, operatorId);
            strictInsertFill(metaObject, "updatedBy", Long.class, operatorId);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        setFieldValByName("updatedAt", LocalDateTime.now(), metaObject);
        Long operatorId = currentOperatorId();
        if (operatorId != null) {
            setFieldValByName("updatedBy", operatorId, metaObject);
        }
    }

    /**
     * 仅在标准后台认证主体存在时填充操作者，避免匿名或系统上下文误写假用户。
     */
    private Long currentOperatorId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUserPrincipal adminUserPrincipal) {
            return adminUserPrincipal.getUserId();
        }
        return null;
    }
}
