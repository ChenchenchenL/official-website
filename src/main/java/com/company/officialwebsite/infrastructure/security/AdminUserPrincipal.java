package com.company.officialwebsite.infrastructure.security;

import com.company.officialwebsite.common.enums.RoleCode;
import com.company.officialwebsite.common.enums.UserStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * AdminUserPrincipal：承载后台用户在 Spring Security 中的认证主体信息。
 */
public class AdminUserPrincipal implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;
    private final String displayName;
    private final String roleCode;
    private final String status;

    public AdminUserPrincipal(
            Long userId,
            String username,
            String password,
            String displayName,
            String roleCode,
            String status) {
        this.userId = userId;
        this.username = username;
        this.password = password;
        this.displayName = displayName;
        this.roleCode = roleCode;
        this.status = status;
    }

    public Long getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRoleCode() {
        return roleCode;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(RoleCode.fromCode(roleCode).toAuthority()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return UserStatus.ENABLED.getCode().equals(status);
    }
}
