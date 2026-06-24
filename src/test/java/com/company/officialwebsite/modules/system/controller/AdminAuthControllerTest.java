package com.company.officialwebsite.modules.system.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * AdminAuthControllerTest：验证后台登录、退出、CSRF 和身份拦截的核心接口契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void currentUser_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20001))
                .andExpect(jsonPath("$.message").value("请先登录"))
                .andExpect(jsonPath("$.traceId").isNotEmpty());
    }

    @Test
    void csrf_shouldIssueTokenAndCookie() throws Exception {
        mockMvc.perform(get("/admin/api/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.headerName").value("X-XSRF-TOKEN"))
                .andExpect(jsonPath("$.data.parameterName").value("_csrf"))
                .andExpect(cookie().exists("XSRF-TOKEN"));
    }

    @Test
    void login_shouldSucceed_whenCredentialsAreCorrect() throws Exception {
        mockMvc.perform(post("/admin/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123456"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.displayName").value("系统管理员"))
                .andExpect(jsonPath("$.data.roleCode").value("ADMINISTRATOR"));
    }

    @Test
    void login_shouldReturnUnauthorized_whenCredentialsAreInvalid() throws Exception {
        mockMvc.perform(post("/admin/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20003))
                .andExpect(jsonPath("$.message").value("用户名或密码错误"));
    }

    @Test
    void login_shouldReturnForbidden_whenCsrfTokenMissing() throws Exception {
        mockMvc.perform(post("/admin/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123456"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(20005))
                .andExpect(jsonPath("$.message").value("请求校验失败，请刷新页面后重试"));
    }

    @Test
    void currentUser_shouldReturnUserInfo_whenSessionIsAuthenticated() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/admin/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/admin/api/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andExpect(jsonPath("$.data.roleCode").value("ADMINISTRATOR"));
    }

    @Test
    void logout_shouldInvalidateSession() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/admin/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/admin/api/auth/logout")
                        .with(csrf())
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        mockMvc.perform(get("/admin/api/auth/me").session(session))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(20001));
    }

    @Test
    void logout_shouldReturnForbidden_whenCsrfTokenMissing() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/admin/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"admin","password":"Admin@123456"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(post("/admin/api/auth/logout").session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(20005));
    }
}
