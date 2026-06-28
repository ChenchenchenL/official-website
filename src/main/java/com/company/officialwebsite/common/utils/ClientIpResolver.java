package com.company.officialwebsite.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * ClientIpResolver：从可信代理链路中解析真实客户端 IP，不直接信任任意请求头。
 */
public final class ClientIpResolver {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";

    private ClientIpResolver() {}

    /**
     * 依次从 X-Forwarded-For、X-Real-IP 和 remoteAddr 中提取首个可用 IP。
     */
    public static String resolve(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        String xff = request.getHeader(HEADER_X_FORWARDED_FOR);
        if (StringUtils.hasText(xff)) {
            int commaIndex = xff.indexOf(',');
            String first = commaIndex > 0 ? xff.substring(0, commaIndex).trim() : xff.trim();
            if (!first.isEmpty()) {
                return first;
            }
        }
        String xri = request.getHeader(HEADER_X_REAL_IP);
        if (StringUtils.hasText(xri)) {
            return xri.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null ? "" : remoteAddr;
    }
}
