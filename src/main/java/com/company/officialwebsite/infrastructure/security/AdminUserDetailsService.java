package com.company.officialwebsite.infrastructure.security;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.system.service.AdminUserQueryService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * AdminUserDetailsService：将后台用户查询能力适配为 Spring Security 的 UserDetailsService。
 */
@Service
public class AdminUserDetailsService implements UserDetailsService {

    private final AdminUserQueryService adminUserQueryService;

    public AdminUserDetailsService(AdminUserQueryService adminUserQueryService) {
        this.adminUserQueryService = adminUserQueryService;
    }

    /**
     * 对外统一隐藏账号是否存在，认证失败均转为 Spring Security 标准的用户名不存在异常。
     */
    @Override
    public UserDetails loadUserByUsername(String username) {
        try {
            return adminUserQueryService.loadPrincipalByUsername(username);
        } catch (BusinessException ex) {
            if (ex.getErrorCode() == ErrorCode.AUTH_LOGIN_FAILED) {
                throw new UsernameNotFoundException(ex.getMessage(), ex);
            }
            throw ex;
        }
    }
}
