package com.company.officialwebsite.common.response;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.trace.TraceContext;

/**
 * ApiResponse：统一结果返回类，所有 Controller 对外响应必须复用该结构。
 *
 * @param <T> data 字段承载的业务响应类型
 */
public class ApiResponse<T> {

    private Integer code;
    private String message;
    private T data;
    private String traceId;

    public ApiResponse() {
    }

    private ApiResponse(Integer code, String message, T data, String traceId) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.traceId = traceId;
    }

    /**
     * 返回无业务数据的成功响应。
     */
    public static <T> ApiResponse<T> success() {
        return success(null);
    }

    /**
     * 返回携带业务数据的成功响应，traceId 从当前请求上下文获取。
     */
    public static <T> ApiResponse<T> success(T data) {
        return of(ErrorCode.SUCCESS, ErrorCode.SUCCESS.getDefaultMessage(), data, TraceContext.getTraceId());
    }

    /**
     * 使用错误码默认消息构造失败响应。
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getDefaultMessage());
    }

    /**
     * 使用业务安全提示构造失败响应，缺省 data 字段为 null。
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message) {
        return fail(errorCode, message, null);
    }

    /**
     * 构造包含业务数据（如冲突数据）的失败响应。
     */
    public static <T> ApiResponse<T> fail(ErrorCode errorCode, String message, T data) {
        return of(errorCode, message, data, TraceContext.getTraceId());
    }

    /**
     * 构造完整响应对象，供异常处理器等需要显式传入 traceId 的场景使用。
     */
    public static <T> ApiResponse<T> of(ErrorCode errorCode, String message, T data, String traceId) {
        return new ApiResponse<>(errorCode.getCode(), message, data, traceId);
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
