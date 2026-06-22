package com.company.officialwebsite.common.trace;

import org.slf4j.MDC;

/**
 * TraceContext：从当前线程日志上下文中读取请求追踪 ID。
 */
public final class TraceContext {

    private TraceContext() {
    }

    /**
     * 获取当前请求 traceId，非 HTTP 请求上下文中可能为空。
     */
    public static String getTraceId() {
        return MDC.get(TraceConstants.TRACE_ID);
    }
}
