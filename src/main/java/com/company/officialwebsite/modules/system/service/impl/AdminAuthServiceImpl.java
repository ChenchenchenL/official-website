package com.company.officialwebsite.modules.system.service.impl;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.infrastructure.audit.SecurityAuditLogger;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.system.converter.AdminUserConverter;
import com.company.officialwebsite.modules.system.service.AdminAuthService;
import com.company.officialwebsite.modules.system.service.AdminUserQueryService;
import com.company.officialwebsite.modules.system.vo.AdminCurrentUserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * AdminAuthServiceImpl：负责后台登录、退出和当前用户信息读取，并统一维护 Session 中的安全上下文。
 */
@Service
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final AdminUserQueryService adminUserQueryService;
    private final SecurityAuditLogger securityAuditLogger;

    public AdminAuthServiceImpl(
            AuthenticationManager authenticationManager,
            SecurityContextRepository securityContextRepository,
            AdminUserQueryService adminUserQueryService,
            SecurityAuditLogger securityAuditLogger) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.adminUserQueryService = adminUserQueryService;
        this.securityAuditLogger = securityAuditLogger;
    }

    /**
     * 登录成功后立即写入 SecurityContext 和 HttpSession，避免后续接口再走匿名状态。
     */
    @Override
    public AdminCurrentUserVO login(
            String username,
            String password,
            HttpServletRequest request,
            HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(username, password));
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            securityContextRepository.saveContext(securityContext, request, response);

            AdminUserPrincipal principal = (AdminUserPrincipal) authentication.getPrincipal();
            securityAuditLogger.logLoginSuccess(principal.getUserId(), principal.getUsername());
            return AdminUserConverter.toCurrentUserVO(principal);
        } catch (DisabledException ex) {
            securityAuditLogger.logLoginFailure(username, ErrorCode.AUTH_ACCOUNT_DISABLED.getDefaultMessage());
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        } catch (BadCredentialsException ex) {
            securityAuditLogger.logLoginFailure(username, ErrorCode.AUTH_LOGIN_FAILED.getDefaultMessage());
            throw new BusinessException(ErrorCode.AUTH_LOGIN_FAILED);
        }
    }

    /**
     * 退出时清理 Session 和 SecurityContext，避免浏览器复用旧登录态。
     */
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        AdminUserPrincipal currentUser = null;
        try {
            currentUser = adminUserQueryService.getCurrentUser();
        } catch (BusinessException ex) {
            if (ex.getErrorCode() != ErrorCode.AUTH_UNAUTHORIZED) {
                throw ex;
            }
        }

        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        SecurityContextHolder.clearContext();

        if (currentUser != null) {
            securityAuditLogger.logLogout(currentUser.getUserId(), currentUser.getUsername());
        }
    }

    @Override
    public AdminCurrentUserVO currentUser() {
        return AdminUserConverter.toCurrentUserVO(adminUserQueryService.getCurrentUser());
    }
}
