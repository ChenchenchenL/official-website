package com.company.officialwebsite.modules.system.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.system.dto.AdminLoginRequestDTO;
import com.company.officialwebsite.modules.system.service.AdminAuthService;
import com.company.officialwebsite.modules.system.vo.AdminCurrentUserVO;
import com.company.officialwebsite.modules.system.vo.CsrfTokenVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminAuthController：提供后台登录、退出、当前用户和 CSRF Token 获取接口。
 */
@RestController
@RequestMapping("/admin/api/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final CookieCsrfTokenRepository csrfTokenRepository;

    public AdminAuthController(
            AdminAuthService adminAuthService,
            CookieCsrfTokenRepository csrfTokenRepository) {
        this.adminAuthService = adminAuthService;
        this.csrfTokenRepository = csrfTokenRepository;
    }

    /**
     * 登录依赖 CSRF Token 保护，前端应先调用 csrf 接口或任意可发放 token 的 GET 接口。
     */
    @PostMapping("/login")
    public ApiResponse<AdminCurrentUserVO> login(
            @Valid @RequestBody AdminLoginRequestDTO requestDTO,
            HttpServletRequest request,
            HttpServletResponse response) {
        AdminCurrentUserVO currentUser = adminAuthService.login(
                requestDTO.getUsername(),
                requestDTO.getPassword(),
                request,
                response);
        return ApiResponse.success(currentUser);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        adminAuthService.logout(request, response);
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<AdminCurrentUserVO> currentUser() {
        return ApiResponse.success(adminAuthService.currentUser());
    }

    /**
     * 显式生成并下发 token 值、Header 名和参数名，便于同域前端在首次登录前完成 CSRF 初始化。
     */
    @GetMapping("/csrf")
    public ApiResponse<CsrfTokenVO> csrf(HttpServletRequest request, HttpServletResponse response) {
        CsrfToken csrfToken = csrfTokenRepository.generateToken(request);
        csrfTokenRepository.saveToken(csrfToken, request, response);

        CsrfTokenVO tokenVO = new CsrfTokenVO();
        tokenVO.setToken(csrfToken.getToken());
        tokenVO.setHeaderName(csrfToken.getHeaderName());
        tokenVO.setParameterName(csrfToken.getParameterName());
        return ApiResponse.success(tokenVO);
    }
}
