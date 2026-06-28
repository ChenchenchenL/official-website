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
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        if (errorCode != ErrorCode.SUCCESS) {
            log.warn("business exception code={} message={}", errorCode.getCode(), ex.getMessage());
        }
        HttpStatus status = resolveHttpStatus(errorCode);
        return ResponseEntity.status(status).body(ApiResponse.fail(errorCode, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<ValidationErrorData>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex) {
        return buildValidationErrorResponse(ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse<ValidationErrorData>> handleBindException(BindException ex) {
        return buildValidationErrorResponse(ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList());
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(Exception ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(
                ErrorCode.COMMON_PARAM_INVALID,
                ErrorCode.COMMON_PARAM_INVALID.getDefaultMessage()));
    }

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
        if (errorCode == ErrorCode.COMMON_PARAM_INVALID
                || errorCode == ErrorCode.MEDIA_FILE_INVALID
                || errorCode == ErrorCode.SITE_LOGO_MEDIA_INVALID
                || errorCode == ErrorCode.SITE_NAVIGATION_TARGET_INVALID
                || errorCode == ErrorCode.SITE_NAVIGATION_LEVEL_INVALID
                || errorCode == ErrorCode.SITE_NAVIGATION_PARENT_INVALID
                || errorCode == ErrorCode.SITE_HOME_BANNER_MEDIA_INVALID
                || errorCode == ErrorCode.SITE_HOME_BANNER_TARGET_INVALID
                || errorCode == ErrorCode.SITE_HOME_METRIC_VALUE_INVALID
                || errorCode == ErrorCode.SITE_HONOR_ICON_INVALID
                || errorCode == ErrorCode.SITE_CLIENT_LOGO_MEDIA_INVALID
                || errorCode == ErrorCode.SITE_STRENGTH_METRIC_ICON_INVALID
                || errorCode == ErrorCode.SITE_AI_CARD_ICON_INVALID
                || errorCode == ErrorCode.SITE_VALUE_CARD_ICON_INVALID
                || errorCode == ErrorCode.PRODUCT_LOGO_INVALID
                || errorCode == ErrorCode.PRODUCT_NAME_DUPLICATE
                || errorCode == ErrorCode.PRODUCT_SOLUTION_ICON_INVALID
                || errorCode == ErrorCode.PRODUCT_SOLUTION_NAME_DUPLICATE
                || errorCode == ErrorCode.CASE_LOGO_INVALID
                || errorCode == ErrorCode.CASE_TITLE_DUPLICATE
                || errorCode == ErrorCode.SITE_TIMELINE_YEAR_INVALID
                || errorCode == ErrorCode.LEAD_STATUS_INVALID
                || errorCode == ErrorCode.LEAD_SUBMIT_RATE_LIMITED
                || errorCode == ErrorCode.LEAD_EXPORT_TOO_LARGE
                || errorCode == ErrorCode.MEDIA_FILE_SIZE_EXCEEDED
                || errorCode == ErrorCode.MEDIA_FILE_TYPE_UNSUPPORTED
                || errorCode == ErrorCode.MEDIA_FILE_SIGNATURE_INVALID
                || errorCode == ErrorCode.MEDIA_UPLOAD_FAILED
                || errorCode == ErrorCode.MEDIA_STORAGE_WRITE_FAILED) {
            return HttpStatus.BAD_REQUEST;
        }
        if (errorCode == ErrorCode.SITE_NAVIGATION_NAME_DUPLICATE
                || errorCode == ErrorCode.SITE_HONOR_NAME_DUPLICATE
                || errorCode == ErrorCode.SITE_CLIENT_LOGO_NAME_DUPLICATE
                || errorCode == ErrorCode.SITE_STRENGTH_METRIC_LABEL_DUPLICATE
                || errorCode == ErrorCode.SITE_AI_CARD_NAME_DUPLICATE
                || errorCode == ErrorCode.SITE_VALUE_CARD_TITLE_DUPLICATE
                || errorCode == ErrorCode.SITE_PROMISE_TAG_TEXT_DUPLICATE
                || errorCode == ErrorCode.LEAD_COOPERATION_DIRECTION_TAG_TEXT_DUPLICATE
                || errorCode == ErrorCode.SITE_CAPABILITY_CATEGORY_NAME_DUPLICATE
                || errorCode == ErrorCode.SITE_CAPABILITY_ITEM_NAME_DUPLICATE
                || errorCode == ErrorCode.COMMON_DUPLICATE_DATA) {
            return HttpStatus.OK;
        }
        if (errorCode == ErrorCode.COMMON_RESOURCE_NOT_FOUND
                || errorCode == ErrorCode.SITE_HONOR_NOT_FOUND
                || errorCode == ErrorCode.SITE_CLIENT_LOGO_NOT_FOUND
                || errorCode == ErrorCode.SITE_STRENGTH_METRIC_NOT_FOUND
                || errorCode == ErrorCode.SITE_AI_CARD_NOT_FOUND
                || errorCode == ErrorCode.SITE_CAPABILITY_CATEGORY_NOT_FOUND
                || errorCode == ErrorCode.SITE_CAPABILITY_ITEM_NOT_FOUND
                || errorCode == ErrorCode.PRODUCT_NOT_FOUND
                || errorCode == ErrorCode.PRODUCT_SOLUTION_NOT_FOUND
                || errorCode == ErrorCode.CASE_NOT_FOUND
                || errorCode == ErrorCode.SITE_TIMELINE_NOT_FOUND
                || errorCode == ErrorCode.SITE_VALUE_CARD_NOT_FOUND
                || errorCode == ErrorCode.SITE_PROMISE_CONTENT_NOT_FOUND
                || errorCode == ErrorCode.SITE_PROMISE_TAG_NOT_FOUND
                || errorCode == ErrorCode.LEAD_CONTACT_INFO_NOT_FOUND
                || errorCode == ErrorCode.LEAD_COOPERATION_DIRECTION_TAG_NOT_FOUND
                || errorCode == ErrorCode.LEAD_RECORD_NOT_FOUND) {
            return HttpStatus.NOT_FOUND;
        }
        return HttpStatus.OK;
    }
}
