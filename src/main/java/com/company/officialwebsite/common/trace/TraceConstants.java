package com.company.officialwebsite.common.trace;

/**
 * TraceConstants：统一维护请求追踪 ID 的 MDC key 和 HTTP Header 名称。
 */
public final class TraceConstants {

    public static final String TRACE_ID = "traceId";
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    private TraceConstants() {
    }
}
