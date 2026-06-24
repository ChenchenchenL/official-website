package com.company.officialwebsite.modules.system.service;

import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;

/**
 * AdminUserQueryService：提供后台用户查询能力，供认证和当前用户读取复用。
 */
public interface AdminUserQueryService {

    AdminUserPrincipal loadPrincipalByUsername(String username);

    AdminUserPrincipal getCurrentUser();
}
