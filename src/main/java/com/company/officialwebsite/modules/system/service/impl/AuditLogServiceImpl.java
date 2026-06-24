package com.company.officialwebsite.modules.system.service.impl;

import com.company.officialwebsite.common.trace.TraceContext;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.system.entity.SysAuditLogEntity;
import com.company.officialwebsite.modules.system.mapper.SysAuditLogMapper;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * AuditLogServiceImpl：将关键后台操作落库为可追溯的审计记录。
 */
@Service
public class AuditLogServiceImpl implements AuditLogService {

    private final SysAuditLogMapper sysAuditLogMapper;
    private final ObjectMapper objectMapper;

    public AuditLogServiceImpl(SysAuditLogMapper sysAuditLogMapper, ObjectMapper objectMapper) {
        this.sysAuditLogMapper = sysAuditLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录站点配置更新的操作者、请求上下文和前后快照，满足后台配置变更审计要求。
     */
    @Override
    public void recordSiteConfigUpdate(Long targetId, Object beforeSnapshot, Object afterSnapshot) {
        recordGenericOperation("SITE", "UPDATE_SITE_CONFIG", "SITE_CONFIG", targetId, beforeSnapshot, afterSnapshot);
    }

    /**
     * 复用统一的审计上下文字段组装逻辑，供站点配置、导航菜单等后台内容操作落库。
     */
    @Override
    public void recordGenericOperation(
            String moduleName,
            String actionName,
            String targetType,
            Long targetId,
            Object beforeSnapshot,
            Object afterSnapshot) {
        SysAuditLogEntity entity = new SysAuditLogEntity();
        entity.setModuleName(moduleName);
        entity.setActionName(actionName);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setResult("SUCCESS");
        entity.setTraceId(TraceContext.getTraceId());
        entity.setOccurredAt(LocalDateTime.now());
        entity.setBeforeSnapshot(toJson(beforeSnapshot));
        entity.setAfterSnapshot(toJson(afterSnapshot));

        AdminUserPrincipal principal = currentPrincipal();
        if (principal != null) {
            entity.setOperatorId(principal.getUserId());
            entity.setOperatorName(principal.getUsername());
        }

        HttpServletRequest request = currentRequest();
        if (request != null) {
            entity.setRequestIp(request.getRemoteAddr());
            entity.setUserAgent(request.getHeader("User-Agent"));
        }

        sysAuditLogMapper.insert(entity);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            // 审计快照序列化失败属于系统级问题，直接中断写入，避免产生不完整审计记录。
            throw new IllegalStateException("Failed to serialize audit snapshot", ex);
        }
    }

    private AdminUserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUserPrincipal adminUserPrincipal) {
            return adminUserPrincipal;
        }
        return null;
    }

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
