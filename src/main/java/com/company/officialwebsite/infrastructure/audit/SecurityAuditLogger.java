package com.company.officialwebsite.infrastructure.audit;

import com.company.officialwebsite.common.trace.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * SecurityAuditLogger：记录登录、退出和失败登录等基础安全审计日志占位实现。
 */
@Component
public class SecurityAuditLogger {

    private static final Logger log = LoggerFactory.getLogger(SecurityAuditLogger.class);

    public void logLoginSuccess(Long userId, String username) {
        log.info("security_audit action=login_success userId={} username={} traceId={}",
                userId, username, TraceContext.getTraceId());
    }

    /**
     * 失败日志只记录用户名和失败原因，不打印密码等敏感数据。
     */
    public void logLoginFailure(String username, String reason) {
        log.warn("security_audit action=login_failure username={} reason={} traceId={}",
                username, reason, TraceContext.getTraceId());
    }

    public void logLogout(Long userId, String username) {
        log.info("security_audit action=logout userId={} username={} traceId={}",
                userId, username, TraceContext.getTraceId());
    }
}
