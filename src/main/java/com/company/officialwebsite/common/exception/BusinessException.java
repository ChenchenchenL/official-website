package com.company.officialwebsite.common.exception;

import com.company.officialwebsite.common.enums.ErrorCode;

/**
 * BusinessException：承载可预期业务失败的统一异常类型。
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object data;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.data = null;
    }

    /**
     * 使用更具体但安全的业务提示覆盖错误码默认消息。
     */
    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.data = null;
    }

    /**
     * 支持在业务异常中携带恢复元数据（例如保存冲突时的最新草稿 VO）。
     */
    public BusinessException(ErrorCode errorCode, String message, Object data) {
        super(message);
        this.errorCode = errorCode;
        this.data = data;
    }

    /**
     * 保留底层异常用于服务端日志定位，对外响应仍只暴露安全提示。
     */
    public BusinessException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.data = null;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getData() {
        return data;
    }
}
