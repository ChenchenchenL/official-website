package com.company.officialwebsite.common.exception;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.FieldErrorDetail;
import com.company.officialwebsite.common.response.ValidationErrorData;
import com.company.officialwebsite.common.trace.TraceContext;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * GlobalExceptionHandler：统一将业务异常、参数异常和未知异常转换为 ApiResponse。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 业务异常已携带稳定错误码，只按错误码映射 HTTP 状态并保留业务安全提示。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = resolveHttpStatus(errorCode);
        return ResponseEntity.status(status).body(ApiResponse.fail(errorCode, ex.getMessage()));
    }

    /**
     * 处理 JSON 请求体 Bean Validation 失败，并返回字段级错误列表。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorData>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        return buildValidationErrorResponse(ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList());
    }

    /**
     * 处理表单参数或查询参数绑定失败，并返回字段级错误列表。
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<ValidationErrorData>> handleBindException(BindException ex) {
        return buildValidationErrorResponse(ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList());
    }

    /**
     * 处理缺失参数、类型不匹配和单参数校验失败等无法定位到字段列表的请求错误。
     */
    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                ErrorCode.COMMON_PARAM_INVALID,
                ErrorCode.COMMON_PARAM_INVALID.getDefaultMessage()));
    }

    /**
     * 未知异常只向调用方返回系统级错误，完整堆栈保留在服务端日志中。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception ex) {
        log.error("Unhandled system exception", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.SYSTEM_ERROR));
    }

    private ResponseEntity<ApiResponse<ValidationErrorData>> buildValidationErrorResponse(
            List<FieldErrorDetail> fieldErrors) {
        ValidationErrorData data = new ValidationErrorData(fieldErrors);
        return ResponseEntity.badRequest().body(ApiResponse.of(
                ErrorCode.COMMON_PARAM_INVALID,
                ErrorCode.COMMON_PARAM_INVALID.getDefaultMessage(),
                data,
                TraceContext.getTraceId()));
    }

    private HttpStatus resolveHttpStatus(ErrorCode errorCode) {
        if (errorCode == ErrorCode.AUTH_UNAUTHORIZED || errorCode == ErrorCode.AUTH_LOGIN_FAILED) {
            return HttpStatus.UNAUTHORIZED;
        }
        if (errorCode == ErrorCode.AUTH_FORBIDDEN
                || errorCode == ErrorCode.AUTH_ACCOUNT_DISABLED
                || errorCode == ErrorCode.AUTH_CSRF_INVALID) {
            return HttpStatus.FORBIDDEN;
        }
        if (errorCode == ErrorCode.COMMON_PARAM_INVALID) {
            return HttpStatus.BAD_REQUEST;
        }
        if (errorCode == ErrorCode.COMMON_RESOURCE_NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.OK;
    }
}
