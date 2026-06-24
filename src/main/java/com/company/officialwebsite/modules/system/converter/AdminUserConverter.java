package com.company.officialwebsite.modules.system.converter;

import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.system.entity.SysUserEntity;
import com.company.officialwebsite.modules.system.vo.AdminCurrentUserVO;

/**
 * AdminUserConverter：负责后台用户实体、安全主体和响应对象之间的转换。
 */
public final class AdminUserConverter {

    private AdminUserConverter() {
    }

    public static AdminUserPrincipal toPrincipal(SysUserEntity entity) {
        return new AdminUserPrincipal(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getDisplayName(),
                entity.getRoleCode(),
                entity.getStatus());
    }

    public static AdminCurrentUserVO toCurrentUserVO(AdminUserPrincipal principal) {
        AdminCurrentUserVO currentUserVO = new AdminCurrentUserVO();
        currentUserVO.setUserId(principal.getUserId());
        currentUserVO.setUsername(principal.getUsername());
        currentUserVO.setDisplayName(principal.getDisplayName());
        currentUserVO.setRoleCode(principal.getRoleCode());
        return currentUserVO;
    }
}
