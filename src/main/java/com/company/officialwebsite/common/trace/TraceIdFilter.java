package com.company.officialwebsite.common.trace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * TraceIdFilter：为每个 HTTP 请求生成或透传 traceId，并写入日志上下文和响应头。
 */
@Component
public class TraceIdFilter extends OncePerRequestFilter {

    private static final int MAX_TRACE_ID_LENGTH = 64;

    /**
     * traceId 必须在请求结束后清理，避免容器线程复用时串到下一个请求。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String traceId = resolveTraceId(request);
        MDC.put(TraceConstants.TRACE_ID, traceId);
        response.setHeader(TraceConstants.TRACE_ID_HEADER, traceId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(TraceConstants.TRACE_ID);
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(TraceConstants.TRACE_ID_HEADER);
        if (StringUtils.hasText(traceId) && traceId.length() <= MAX_TRACE_ID_LENGTH) {
            return traceId;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
