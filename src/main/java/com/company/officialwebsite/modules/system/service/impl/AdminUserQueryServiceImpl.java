package com.company.officialwebsite.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.system.converter.AdminUserConverter;
import com.company.officialwebsite.modules.system.entity.SysUserEntity;
import com.company.officialwebsite.modules.system.mapper.SysUserMapper;
import com.company.officialwebsite.modules.system.service.AdminUserQueryService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * AdminUserQueryServiceImpl：基于 sys_user 表提供后台用户读取和当前会话主体获取能力。
 */
@Service
public class AdminUserQueryServiceImpl implements AdminUserQueryService {

    private final SysUserMapper sysUserMapper;

    public AdminUserQueryServiceImpl(SysUserMapper sysUserMapper) {
        this.sysUserMapper = sysUserMapper;
    }

    /**
     * 认证阶段只读取活跃账号，逻辑删除过滤统一由 MyBatis-Plus 规则负责。
     */
    @Override
    public AdminUserPrincipal loadPrincipalByUsername(String username) {
        LambdaQueryWrapper<SysUserEntity> queryWrapper = new LambdaQueryWrapper<SysUserEntity>()
                .eq(SysUserEntity::getUsername, username);
        SysUserEntity userEntity = sysUserMapper.selectOne(queryWrapper);
        if (userEntity == null) {
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }
        return AdminUserConverter.toPrincipal(userEntity);
    }

    /**
     * 当前用户信息统一从 SecurityContext 读取，避免控制器自行解析 Session。
     */
    @Override
    public AdminUserPrincipal getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AdminUserPrincipal principal)) {
            throw new BusinessException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return principal;
    }
}
