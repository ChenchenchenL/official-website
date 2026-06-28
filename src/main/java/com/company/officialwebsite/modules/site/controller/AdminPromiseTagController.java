package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.PromiseTagBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.PromiseTagCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.PromiseTagUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.PromiseTagService;
import com.company.officialwebsite.modules.site.vo.AdminPromiseTagVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminPromiseTagController：提供后台承诺标签管理接口。
 */
@Validated
@RestController
@RequestMapping("/admin/api/promise-tags")
public class AdminPromiseTagController {

    private static final Logger log = LoggerFactory.getLogger(AdminPromiseTagController.class);

    private final PromiseTagService promiseTagService;

    public AdminPromiseTagController(PromiseTagService promiseTagService) {
        this.promiseTagService = promiseTagService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminPromiseTagVO>> getPromiseTags(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get promise tags request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(promiseTagService.getAdminPromiseTagList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createPromiseTag(@Valid @RequestBody PromiseTagCreateRequestDTO requestDTO) {
        log.info("create promise tag request tagText={}", requestDTO.getTagText());
        promiseTagService.createPromiseTag(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updatePromiseTag(
            @PathVariable Long id,
            @Valid @RequestBody PromiseTagUpdateRequestDTO requestDTO) {
        log.info("update promise tag request id={} version={} tagText={}",
                id, requestDTO.getVersion(), requestDTO.getTagText());
        promiseTagService.updatePromiseTag(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deletePromiseTag(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        log.info("delete promise tag request id={} version={}", id, version);
        promiseTagService.deletePromiseTag(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderPromiseTags(@Valid @RequestBody PromiseTagBatchSortRequestDTO requestDTO) {
        log.info("reorder promise tags request count={}",
                requestDTO.getOrderedPromiseTagIds() == null ? 0 : requestDTO.getOrderedPromiseTagIds().size());
        promiseTagService.reorderPromiseTags(requestDTO);
        return ApiResponse.success();
    }
}
