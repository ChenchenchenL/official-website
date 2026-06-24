package com.company.officialwebsite.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;
import java.time.LocalDateTime;

/**
 * SysAuditLogEntity：记录后台关键操作的审计日志。
 */
@TableName("sys_audit_log")
public class SysAuditLogEntity extends BaseEntity {

    private String moduleName;
    private String actionName;
    private String targetType;
    private Long targetId;
    private Long operatorId;
    private String operatorName;
    private String requestIp;
    private String userAgent;
    private String result;
    private String traceId;
    private LocalDateTime occurredAt;

    @TableField("before_snapshot")
    private String beforeSnapshot;

    @TableField("after_snapshot")
    private String afterSnapshot;

    public String getModuleName() {
        return moduleName;
    }

    public void setModuleName(String moduleName) {
        this.moduleName = moduleName;
    }

    public String getActionName() {
        return actionName;
    }

    public void setActionName(String actionName) {
        this.actionName = actionName;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public void setOperatorName(String operatorName) {
        this.operatorName = operatorName;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(LocalDateTime occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getBeforeSnapshot() {
        return beforeSnapshot;
    }

    public void setBeforeSnapshot(String beforeSnapshot) {
        this.beforeSnapshot = beforeSnapshot;
    }

    public String getAfterSnapshot() {
        return afterSnapshot;
    }

    public void setAfterSnapshot(String afterSnapshot) {
        this.afterSnapshot = afterSnapshot;
    }
}
