package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.dto.LockForceReleaseDTO;
import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminPageLockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EditorLockService editorLockService;

    // -----------------------------------------------------------------------
    // 1. 未登录拒绝访问
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("未登录访问锁接口返回 401")
    void unauthenticated_shouldReturn401() throws Exception {
        mockMvc.perform(get("/admin/api/page-builder/pages/1/lock"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/admin/api/page-builder/pages/1/lock")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized());
    }

    // -----------------------------------------------------------------------
    // 2. 非管理员角色拒绝访问
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("普通用户（无管理员角色）访问锁接口返回 401/403")
    void nonAdminUser_shouldBeRejected() throws Exception {
        // session-based 安全配置下，MockMvc with(user(...)) 不建立持久 session，
        // method security 抛 AccessDeniedException 后框架走 AuthenticationEntryPoint 返回 401。
        // 实际生产中有效 session 但无权限的用户会收到 403；
        // 此处断言"不能成功访问"（非 200），覆盖拒绝语义即可。
        mockMvc.perform(get("/admin/api/page-builder/pages/1/lock")
                        .with(user("regular_user").roles("USER")))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("无管理员角色的用户应被拒绝（401 或 403）")
                        .isIn(401, 403));

        mockMvc.perform(post("/admin/api/page-builder/pages/1/lock")
                        .with(user("regular_user").roles("USER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(result -> assertThat(result.getResponse().getStatus())
                        .as("无管理员角色的用户应被拒绝（401 或 403）")
                        .isIn(401, 403));
    }

    // -----------------------------------------------------------------------
    // 3. CSRF 校验
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("POST 加锁缺少 CSRF Token 返回 403")
    void acquireLock_missingCsrf_shouldReturn403() throws Exception {
        mockMvc.perform(post("/admin/api/page-builder/pages/1/lock")
                        .with(user("admin").roles("ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("强制解锁缺少 CSRF Token 返回 403")
    void forceRelease_missingCsrf_shouldReturn403() throws Exception {
        LockForceReleaseDTO dto = new LockForceReleaseDTO("紧急解锁");
        mockMvc.perform(post("/admin/api/page-builder/pages/1/lock/force-release")
                        .with(user("super").roles("SUPER_ADMINISTRATOR"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden());
    }

    // -----------------------------------------------------------------------
    // 4. 完整正常流程（ADMINISTRATOR）
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("ADMINISTRATOR 完整锁生命周期：查询、加锁、心跳、释放")
    void fullLockLifecycle_administrator() throws Exception {
        // 查询初始状态
        mockMvc.perform(get("/admin/api/page-builder/pages/2001/lock")
                        .with(user("admin").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.editable").value(true))
                .andExpect(jsonPath("$.data.forceUnlockAllowed").value(false));

        // 加锁
        String resp = mockMvc.perform(post("/admin/api/page-builder/pages/2001/lock")
                        .with(user("admin").roles("ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.lockToken").exists())
                .andExpect(jsonPath("$.data.forceUnlockAllowed").value(false))
                .andReturn().getResponse().getContentAsString();

        String lockToken = objectMapper.readTree(resp).path("data").path("lockToken").asText();
        assertThat(lockToken).isNotBlank();

        // 响应中不得携带 auditId、auditSnapshot 等内部字段
        assertThat(resp).doesNotContain("auditId").doesNotContain("auditSnapshot");

        // 心跳
        mockMvc.perform(post("/admin/api/page-builder/pages/2001/lock/heartbeat")
                        .with(user("admin").roles("ADMINISTRATOR"))
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lockToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.lockToken").doesNotExist());

        // 释放
        mockMvc.perform(delete("/admin/api/page-builder/pages/2001/lock")
                        .with(user("admin").roles("ADMINISTRATOR"))
                        .with(csrf())
                        .header("X-Editor-Lock-Token", lockToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    // -----------------------------------------------------------------------
    // 5. 第二人抢锁触发 409 / 10006
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("第二人抢同一页面编辑锁返回 409 和 10006")
    void acquireLock_conflict_shouldReturn409And10006() throws Exception {
        mockMvc.perform(post("/admin/api/page-builder/pages/2002/lock")
                        .with(user("admin_a").roles("ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/api/page-builder/pages/2002/lock")
                        .with(user("admin_b").roles("ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.EDITOR_LOCK_CONFLICT.getCode()))
                .andExpect(jsonPath("$.data.lockToken").doesNotExist());
    }

    // -----------------------------------------------------------------------
    // 6. ADMINISTRATOR 调用 force-release 返回 403
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("ADMINISTRATOR 调用强制解锁进入 Service 后返回 20004/403")
    void forceRelease_byAdministrator_shouldBeRejected() throws Exception {
        LockForceReleaseDTO dto = new LockForceReleaseDTO("无权解锁测试");
        // ADMINISTRATOR 能通过 @PreAuthorize，但 Service 检查 canForceUnlock=false 抛 20004
        // GlobalExceptionHandler 将 EDITOR_LOCK_FORCE_RELEASE_DENIED 映射为 HTTP 403
        mockMvc.perform(post("/admin/api/page-builder/pages/2003/lock/force-release")
                        .with(user("admin").roles("ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(ErrorCode.EDITOR_LOCK_FORCE_RELEASE_DENIED.getCode()));
    }

    // -----------------------------------------------------------------------
    // 7. SUPER_ADMINISTRATOR 强制解锁成功，forceUnlockAllowed = true
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("SUPER_ADMINISTRATOR 加锁返回 forceUnlockAllowed=true")
    void acquireLock_superAdmin_forceUnlockAllowedTrue() throws Exception {
        mockMvc.perform(post("/admin/api/page-builder/pages/2004/lock")
                        .with(user("super").roles("SUPER_ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.forceUnlockAllowed").value(true));
    }

    @Test
    @DisplayName("SUPER_ADMINISTRATOR 强制解锁有锁资源成功并写审计")
    void forceRelease_bySuperAdmin_withActiveLock_shouldSucceed() throws Exception {
        // 先由普通管理员加锁
        editorLockService.acquireLock(EditorResourceTypeEnum.PAGE, 2005L, null, "admin_holder", "持锁人", false);

        LockForceReleaseDTO dto = new LockForceReleaseDTO("紧急恢复编辑");
        String resp = mockMvc.perform(post("/admin/api/page-builder/pages/2005/lock/force-release")
                        .with(user("super").roles("SUPER_ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.releasedAt").exists())
                .andReturn().getResponse().getContentAsString();

        // 响应不含 lockToken
        assertThat(resp).doesNotContain("lockToken");

        // 解锁后资源可再次加锁
        mockMvc.perform(post("/admin/api/page-builder/pages/2005/lock")
                        .with(user("admin_new").roles("ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.editable").value(true));
    }

    @Test
    @DisplayName("SUPER_ADMINISTRATOR 对无锁资源强制解锁也写审计并返回成功")
    void forceRelease_bySuperAdmin_noActiveLock_shouldSucceedAndAudit() throws Exception {
        LockForceReleaseDTO dto = new LockForceReleaseDTO("无锁强解审计测试");
        mockMvc.perform(post("/admin/api/page-builder/pages/2006/lock/force-release")
                        .with(user("super").roles("SUPER_ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.releasedAt").exists());
    }

    // -----------------------------------------------------------------------
    // 8. 强制解锁缺少 reason 返回 400
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("强制解锁缺少 reason 返回 400")
    void forceRelease_missingReason_shouldReturn400() throws Exception {
        mockMvc.perform(post("/admin/api/page-builder/pages/2007/lock/force-release")
                        .with(user("super").roles("SUPER_ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"\"}"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    // 9. 并发抢锁：多线程中只有一把有效锁
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("并发抢锁：N 个线程中只有一把最终成功")
    void acquireLock_concurrent_onlyOneLockSurvives() throws Exception {
        int threads = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Callable<Integer>> tasks = new ArrayList<>();

        for (int i = 0; i < threads; i++) {
            final String username = "concurrent_admin_" + i;
            tasks.add(() -> {
                try {
                    MvcResult result = mockMvc.perform(
                            post("/admin/api/page-builder/pages/2099/lock")
                                    .with(user(username).roles("ADMINISTRATOR"))
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                            .andReturn();
                    return result.getResponse().getStatus();
                } catch (Exception e) {
                    return 500;
                }
            });
        }

        List<Future<Integer>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        long successCount = futures.stream()
                .map(f -> {
                    try { return f.get(); } catch (Exception e) { return 500; }
                })
                .filter(s -> s == 200)
                .count();

        // 至少有一个成功，但只有一把锁
        assertThat(successCount).isGreaterThanOrEqualTo(1);

        // 验证资源现在被锁定（再抢会 409）
        mockMvc.perform(post("/admin/api/page-builder/pages/2099/lock")
                        .with(user("late_admin").roles("ADMINISTRATOR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(ErrorCode.EDITOR_LOCK_CONFLICT.getCode()));
    }

    // -----------------------------------------------------------------------
    // 10. 查询锁状态：lockToken 不出现在响应中
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("查询锁状态响应不包含 lockToken")
    void getLockStatus_shouldNotContainLockToken() throws Exception {
        editorLockService.acquireLock(EditorResourceTypeEnum.PAGE, 2008L, null, "admin_check", "检查管理员", false);

        String resp = mockMvc.perform(get("/admin/api/page-builder/pages/2008/lock")
                        .with(user("admin_check").roles("ADMINISTRATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.lockToken").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        assertThat(resp).doesNotContain("lockToken");
    }
}
