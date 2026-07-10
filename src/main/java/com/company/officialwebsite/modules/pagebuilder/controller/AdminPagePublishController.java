package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.pagebuilder.dto.PagePublishDTO;
import com.company.officialwebsite.modules.pagebuilder.service.PagePublishService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageVersionVO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AdminPagePublishController: 后台页面发布、回滚与版本历史管理接口。
 */
@RestController
@RequestMapping("/admin/api/page-builder/pages")
@Validated
public class AdminPagePublishController {

    private static final Logger log = LoggerFactory.getLogger(AdminPagePublishController.class);

    private final PagePublishService pagePublishService;

    public AdminPagePublishController(PagePublishService pagePublishService) {
        this.pagePublishService = pagePublishService;
    }

    /**
     * 将指定页面草稿发布上线。
     */
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageVersionVO> publishPage(
            @PathVariable Long id,
            @Valid @RequestBody PagePublishDTO dto) {
        log.info("Admin request: publish page id={}", id);
        return ApiResponse.success(pagePublishService.publishPage(id, dto));
    }

    /**
     * 将指定页面回滚到历史发布版本。
     */
    @PostMapping("/{id}/rollback")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageVersionVO> rollbackPage(
            @PathVariable Long id,
            @RequestParam("versionId") Long versionId) {
        log.info("Admin request: rollback page id={} to versionId={}", id, versionId);
        return ApiResponse.success(pagePublishService.rollbackPage(id, versionId));
    }

    /**
     * 查询指定页面定义的全部历史发布版本。
     */
    @GetMapping("/{id}/versions")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<PageVersionVO>> getVersions(@PathVariable Long id) {
        log.info("Admin request: get versions list for page id={}", id);
        return ApiResponse.success(pagePublishService.listVersions(id));
    }
}
