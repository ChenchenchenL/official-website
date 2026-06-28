package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.PromiseContentUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.PromiseContentService;
import com.company.officialwebsite.modules.site.vo.AdminPromiseContentVO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminPromiseContentController：提供后台"我们的承诺"主体宣导文案管理接口。
 */
@Validated
@RestController
@RequestMapping("/admin/api/promise-content")
public class AdminPromiseContentController {

    private static final Logger log = LoggerFactory.getLogger(AdminPromiseContentController.class);

    private final PromiseContentService promiseContentService;

    public AdminPromiseContentController(PromiseContentService promiseContentService) {
        this.promiseContentService = promiseContentService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminPromiseContentVO> getPromiseContent() {
        log.info("get promise content request");
        return ApiResponse.success(promiseContentService.getAdminPromiseContent());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updatePromiseContent(@Valid @RequestBody PromiseContentUpdateRequestDTO requestDTO) {
        log.info("update promise content request version={}", requestDTO.getVersion());
        promiseContentService.updatePromiseContent(requestDTO);
        return ApiResponse.success();
    }
}
