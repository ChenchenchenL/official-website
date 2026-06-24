package com.company.officialwebsite.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.trace.TraceConstants;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.DefaultCorsProcessor;

/**
 * CorsConfigurationTest：验证 Admin 与 Portal 跨域白名单、凭证策略和请求头放行规则。
 */
class CorsConfigurationTest {

    @Test
    void corsConfigurationSource_shouldAllowAdminOriginWithCredentialsAndCsrfHeader() throws Exception {
        OfficialProperties officialProperties = buildOfficialProperties();
        CorsConfiguration configuration = new CorsConfiguration();
        CorsConfigurationSource source = configuration.corsConfigurationSource(officialProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/admin/api/auth/login");
        request.addHeader("Origin", "https://admin.test.example.com");
        request.addHeader("Access-Control-Request-Method", "POST");
        request.addHeader("Access-Control-Request-Headers", "X-XSRF-TOKEN,Content-Type");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = new DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, response);

        assertThat(handled).isTrue();
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://admin.test.example.com");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isEqualTo("true");
        assertThat(response.getHeader("Access-Control-Allow-Headers"))
                .contains("X-XSRF-TOKEN")
                .contains("Content-Type");
        assertThat(response.getHeader("Access-Control-Max-Age")).isEqualTo("1800");
    }

    @Test
    void corsConfigurationSource_shouldAllowPortalOriginPatternWithoutCredentials() throws Exception {
        OfficialProperties officialProperties = buildOfficialProperties();
        CorsConfiguration configuration = new CorsConfiguration();
        CorsConfigurationSource source = configuration.corsConfigurationSource(officialProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/portal/api/home");
        request.addHeader("Origin", "https://www.test.example.com");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = new DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, response);

        assertThat(handled).isTrue();
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isEqualTo("https://www.test.example.com");
        assertThat(response.getHeader("Access-Control-Allow-Credentials")).isNull();
        assertThat(response.getHeader("Access-Control-Expose-Headers")).contains(TraceConstants.TRACE_ID_HEADER);
    }

    @Test
    void corsConfigurationSource_shouldRejectOriginOutsideWhitelist() throws Exception {
        OfficialProperties officialProperties = buildOfficialProperties();
        CorsConfiguration configuration = new CorsConfiguration();
        CorsConfigurationSource source = configuration.corsConfigurationSource(officialProperties);

        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/admin/api/auth/login");
        request.addHeader("Origin", "https://evil.example.com");
        request.addHeader("Access-Control-Request-Method", "POST");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean handled = new DefaultCorsProcessor().processRequest(source.getCorsConfiguration(request), request, response);

        assertThat(handled).isFalse();
        assertThat(response.getHeader("Access-Control-Allow-Origin")).isNull();
    }

    private OfficialProperties buildOfficialProperties() {
        OfficialProperties officialProperties = new OfficialProperties();
        officialProperties.getSecurity().setCsrfHeaderName("X-XSRF-TOKEN");
        officialProperties.getCors().getAdmin().setAllowedOrigins(List.of("https://admin.test.example.com"));
        officialProperties.getCors().getAdmin().setMaxAge(Duration.ofMinutes(30));
        officialProperties.getCors().getPortal().setAllowedOriginPatterns(List.of("https://*.test.example.com"));
        officialProperties.getCors().getPortal().setMaxAge(Duration.ofMinutes(15));
        return officialProperties;
    }
}
