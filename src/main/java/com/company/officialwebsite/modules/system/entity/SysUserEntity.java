package com.company.officialwebsite.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;
import java.time.LocalDateTime;

/**
 * SysUserEntity：映射后台用户表 sys_user，承载账号、角色和登录状态信息。
 */
@TableName("sys_user")
public class SysUserEntity extends BaseEntity {

    private String username;

    @TableField("password_hash")
    private String passwordHash;

    private String displayName;

    private String roleCode;

    private String status;

    private LocalDateTime lastLoginAt;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }
}
