package com.company.officialwebsite.infrastructure.security;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.trace.TraceConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * RestAccessDeniedHandler：将权限不足和 CSRF 拒绝统一转换为 JSON 响应。
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * CSRF 拒绝需要单独映射错误码，便于前端识别后主动刷新 token。
     */
    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        ErrorCode errorCode = accessDeniedException instanceof CsrfException
                ? ErrorCode.AUTH_CSRF_INVALID
                : ErrorCode.AUTH_FORBIDDEN;
        String traceId = resolveTraceId(request, response);

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                null,
                traceId));
    }

    private String resolveTraceId(HttpServletRequest request, HttpServletResponse response) {
        String traceId = response.getHeader(TraceConstants.TRACE_ID_HEADER);
        if (!StringUtils.hasText(traceId)) {
            traceId = request.getHeader(TraceConstants.TRACE_ID_HEADER);
        }
        if (!StringUtils.hasText(traceId)) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        response.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);
        return traceId;
    }
}
