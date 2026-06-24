package com.company.officialwebsite.modules.system.service;

import com.company.officialwebsite.modules.system.vo.AdminCurrentUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * AdminAuthService：封装后台登录态写入、退出和当前用户查询逻辑。
 */
public interface AdminAuthService {

    AdminCurrentUserVO login(String username, String password, HttpServletRequest request, HttpServletResponse response);

    void logout(HttpServletRequest request, HttpServletResponse response);

    AdminCurrentUserVO currentUser();
}
