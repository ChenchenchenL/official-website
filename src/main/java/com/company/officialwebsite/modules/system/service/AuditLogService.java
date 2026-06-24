package com.company.officialwebsite.modules.system.service;

/**
 * AuditLogService：封装后台关键操作的审计落库能力。
 */
public interface AuditLogService {

    /**
     * 记录站点基础配置更新的前后快照。
     */
    void recordSiteConfigUpdate(Long targetId, Object beforeSnapshot, Object afterSnapshot);

    /**
     * 记录通用后台内容操作的前后快照。
     */
    void recordGenericOperation(
            String moduleName,
            String actionName,
            String targetType,
            Long targetId,
            Object beforeSnapshot,
            Object afterSnapshot);
}
