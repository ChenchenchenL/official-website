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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * RestAuthenticationEntryPoint：将未登录访问后台接口的结果转换为统一 JSON 响应。
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException, ServletException {
        String traceId = resolveTraceId(request, response);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.of(
                ErrorCode.AUTH_UNAUTHORIZED,
                ErrorCode.AUTH_UNAUTHORIZED.getDefaultMessage(),
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
