package com.company.officialwebsite.modules.product.controller;

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
 * AdminProductLockController：产品详情独占编辑锁管理接口。
 */
@RestController
@RequestMapping("/admin/api/products/{id}/lock")
public class AdminProductLockController {

    private static final Logger log = LoggerFactory.getLogger(AdminProductLockController.class);

    private final EditorLockService editorLockService;

    public AdminProductLockController(EditorLockService editorLockService) {
        this.editorLockService = editorLockService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<LockStatusVO> getLockStatus(
            @PathVariable Long id,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        log.info("admin query product lock status id={} user={}", id, username);
        return ApiResponse.success(editorLockService.getLockStatus(EditorResourceTypeEnum.PRODUCT, id, username, hasForceUnlockRole(authentication)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<LockStatusVO> acquireLock(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) LockAcquireDTO dto,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        log.info("admin acquire product lock id={} user={}", id, username);
        return ApiResponse.success(editorLockService.acquireLock(EditorResourceTypeEnum.PRODUCT, id, dto, username, username, hasForceUnlockRole(authentication)));
    }

    @PostMapping("/heartbeat")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<LockStatusVO> heartbeat(
            @PathVariable Long id,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        return ApiResponse.success(editorLockService.heartbeat(EditorResourceTypeEnum.PRODUCT, id, lockToken, username));
    }

    @DeleteMapping
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<Void> releaseLock(
            @PathVariable Long id,
            @RequestHeader(value = "X-Editor-Lock-Token", required = false) String lockToken,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        log.info("admin release product lock id={} user={}", id, username);
        editorLockService.releaseLock(EditorResourceTypeEnum.PRODUCT, id, lockToken, username);
        return ApiResponse.success();
    }

    @PostMapping("/force-release")
    @PreAuthorize("hasRole('ADMINISTRATOR') or hasRole('SUPER_ADMINISTRATOR')")
    public ApiResponse<Map<String, Object>> forceRelease(
            @PathVariable Long id,
            @Valid @RequestBody LockForceReleaseDTO dto,
            Authentication authentication) {
        String username = resolveUsername(authentication);
        log.info("admin force release product lock id={} user={}", id, username);
        return ApiResponse.success(editorLockService.forceRelease(EditorResourceTypeEnum.PRODUCT, id, dto, username, hasForceUnlockRole(authentication)));
    }

    private String resolveUsername(Authentication authentication) {
        if (authentication == null) return "anonymous";
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUserPrincipal adminUser) return adminUser.getUsername();
        if (principal instanceof UserDetails userDetails) return userDetails.getUsername();
        return authentication.getName() != null ? authentication.getName() : "anonymous";
    }

    private boolean hasForceUnlockRole(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_SUPER_ADMINISTRATOR".equals(a.getAuthority()));
    }
}
