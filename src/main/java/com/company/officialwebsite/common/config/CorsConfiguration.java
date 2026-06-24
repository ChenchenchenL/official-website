package com.company.officialwebsite.common.config;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.constants.SecurityConstants;
import com.company.officialwebsite.common.trace.TraceConstants;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CorsConfiguration：统一注册 Admin 与 Portal 的跨域白名单，避免生产环境出现危险的全局放开。
 */
@Configuration
public class CorsConfiguration {

    private static final List<String> ALLOWED_METHODS = List.of(
            HttpMethod.GET.name(),
            HttpMethod.POST.name(),
            HttpMethod.PUT.name(),
            HttpMethod.PATCH.name(),
            HttpMethod.DELETE.name(),
            HttpMethod.OPTIONS.name());

    /**
     * Cookie Session 的后台接口与公开 Portal 接口必须分别配置，避免两类调用端互相牵制。
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(OfficialProperties officialProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(
                SecurityConstants.ADMIN_API_PATTERN,
                buildCorsConfiguration(officialProperties.getCors().getAdmin(), true, officialProperties));
        source.registerCorsConfiguration(
                SecurityConstants.PORTAL_API_PATTERN,
                buildCorsConfiguration(officialProperties.getCors().getPortal(), false, officialProperties));
        return source;
    }

    private org.springframework.web.cors.CorsConfiguration buildCorsConfiguration(
            OfficialProperties.Scope scope,
            boolean allowCredentials,
            OfficialProperties officialProperties) {
        org.springframework.web.cors.CorsConfiguration corsConfiguration = new org.springframework.web.cors.CorsConfiguration();
        corsConfiguration.setAllowCredentials(allowCredentials);
        corsConfiguration.setAllowedMethods(ALLOWED_METHODS);
        corsConfiguration.setAllowedHeaders(buildAllowedHeaders(officialProperties));
        corsConfiguration.setExposedHeaders(List.of(TraceConstants.TRACE_ID_HEADER));
        corsConfiguration.setMaxAge(scope.getMaxAge().getSeconds());
        corsConfiguration.setAllowedOrigins(scope.getAllowedOrigins());
        corsConfiguration.setAllowedOriginPatterns(scope.getAllowedOriginPatterns());
        return corsConfiguration;
    }

    private List<String> buildAllowedHeaders(OfficialProperties officialProperties) {
        LinkedHashSet<String> headers = new LinkedHashSet<>();
        headers.add(HttpHeaders.CONTENT_TYPE);
        headers.add(HttpHeaders.ACCEPT);
        headers.add(HttpHeaders.ORIGIN);
        headers.add(HttpHeaders.AUTHORIZATION);
        headers.add("X-Requested-With");
        headers.add(TraceConstants.TRACE_ID_HEADER);
        headers.add(officialProperties.getSecurity().getCsrfHeaderName());
        return new ArrayList<>(headers);
    }
}
