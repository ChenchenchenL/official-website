package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.dto.LockAcquireDTO;
import com.company.officialwebsite.common.dto.LockForceReleaseDTO;
import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.vo.LockStatusVO;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AdminPageLockController：页面构建器独占编辑锁管理接口。
 */
@RestController
@RequestMapping("/admin/api/page-builder/pages/{pageId}/lock")
public class AdminPageLockController {

    private static final Logger log = LoggerFactory.getLogger(AdminPageLockController.class);

    private final EditorLockService editorLockService;

    public AdminPageLockController(EditorLockService editorLockService) {
        this.editorLockService = editorLockService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<LockStatusVO> getLockStatus(
            @PathVariable Long pageId,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        boolean canForceUnlock = hasForceUnlockRole(authentication);
        log.info("admin query page lock status pageId={} user={}", pageId, username);
        return ApiResponse.success(editorLockService.getLockStatus(EditorResourceTypeEnum.PAGE, pageId, username, canForceUnlock));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<LockStatusVO> acquireLock(
            @PathVariable Long pageId,
            @Valid @RequestBody(required = false) LockAcquireDTO dto,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        boolean canForceUnlock = hasForceUnlockRole(authentication);
        log.info("admin acquire page lock pageId={} user={}", pageId, username);
        return ApiResponse.success(editorLockService.acquireLock(EditorResourceTypeEnum.PAGE, pageId, dto, username, username, canForceUnlock));
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<LockStatusVO> heartbeat(
            @PathVariable Long pageId,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        return ApiResponse.success(editorLockService.heartbeat(EditorResourceTypeEnum.PAGE, pageId, lockToken, username));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<Void> releaseLock(
            @PathVariable Long pageId,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        log.info("admin release page lock pageId={} user={}", pageId, username);
        editorLockService.releaseLock(EditorResourceTypeEnum.PAGE, pageId, lockToken, username);
        return ApiResponse.success();
    }

    @PostMapping("/force-release")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<Map<String, Object>> forceRelease(
            @PathVariable Long pageId,
            @Valid @RequestBody LockForceReleaseDTO dto,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        log.info("admin force release page lock pageId={} user={}", pageId, username);
        return ApiResponse.success(editorLockService.forceRelease(EditorResourceTypeEnum.PAGE, pageId, dto, username, hasForceUnlockRole(authentication)));
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null) {
            return "anonymous";
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUserPrincipal adminUser) {
            return adminUser.getUsername();
        }
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return authentication.getName() != null ? authentication.getName() : "anonymous";
    }

    private boolean hasForceUnlockRole(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMINISTRATOR".equals(a.getAuthority()));
    }
}
