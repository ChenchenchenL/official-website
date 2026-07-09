package com.company.officialwebsite.common.config;

import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.constants.SecurityConstants;
import com.company.officialwebsite.infrastructure.security.RestAccessDeniedHandler;
import com.company.officialwebsite.infrastructure.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * SecurityConfiguration：配置后台登录、会话鉴权、CSRF 防护、跨域策略和未授权响应格式。
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfiguration {

    /**
     * 统一使用 Cookie 形式的 CSRF Token，兼容同域前后端通过 Header 提交。
     */
    @Bean
    public CookieCsrfTokenRepository csrfTokenRepository(OfficialProperties officialProperties) {
        CookieCsrfTokenRepository csrfTokenRepository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        csrfTokenRepository.setCookieName(officialProperties.getSecurity().getCsrfCookieName());
        csrfTokenRepository.setHeaderName(officialProperties.getSecurity().getCsrfHeaderName());
        csrfTokenRepository.setCookieCustomizer(cookieBuilder -> cookieBuilder
                .httpOnly(false)
                .path(officialProperties.getSecurity().getCsrfCookiePath())
                .sameSite(officialProperties.getSecurity().getCsrfCookieSameSite()));
        return csrfTokenRepository;
    }

    /**
     * 后台管理接口默认拒绝匿名访问，仅开放登录和获取 CSRF Token 所需白名单。
     */
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity httpSecurity,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler,
            CookieCsrfTokenRepository csrfTokenRepository) throws Exception {
        CsrfTokenRequestAttributeHandler csrfTokenRequestHandler = new CsrfTokenRequestAttributeHandler();
        csrfTokenRequestHandler.setCsrfRequestAttributeName(null);

        httpSecurity
                .securityContext(securityContext -> securityContext.requireExplicitSave(false))
                .sessionManagement(sessionManagement -> sessionManagement
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(csrfTokenRepository)
                        .csrfTokenRequestHandler(csrfTokenRequestHandler)
                        .ignoringRequestMatchers(
                                SecurityConstants.PORTAL_API_PATTERN,
                                SecurityConstants.LOGIN_ENDPOINT))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(SecurityConstants.CSRF_ENDPOINT, SecurityConstants.LOGIN_ENDPOINT, "/error")
                        .permitAll()
                        .requestMatchers(SecurityConstants.PORTAL_API_PATTERN)
                        .permitAll()
                        .requestMatchers(SecurityConstants.ADMIN_API_PATTERN)
                        .authenticated()
                        .anyRequest()
                        .permitAll())
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .anonymous(Customizer.withDefaults());

        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_THREADLOCAL);
        return httpSecurity.build();
    }

    /**
     * 密码统一使用 BCrypt，避免出现弱哈希或明文密码存储。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * 复用 Spring Security 自动装配的认证链，便于控制器显式发起登录认证。
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * 登录成功后统一将 SecurityContext 持久化到 HttpSession，避免出现多个会话保存实现。
     */
    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }
}
