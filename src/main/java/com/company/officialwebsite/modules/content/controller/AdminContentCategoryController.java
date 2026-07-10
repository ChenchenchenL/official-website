package com.company.officialwebsite.modules.content.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.content.dto.ContentCategoryBatchSortRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentCategoryCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentCategoryUpdateRequestDTO;
import com.company.officialwebsite.modules.content.service.ContentCategoryService;
import com.company.officialwebsite.modules.content.vo.AdminContentCategoryVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
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

@Validated
@RestController
@RequestMapping("/admin/api/content-categories")
public class AdminContentCategoryController {

    private static final Logger log = LoggerFactory.getLogger(AdminContentCategoryController.class);

    private final ContentCategoryService contentCategoryService;

    public AdminContentCategoryController(ContentCategoryService contentCategoryService) {
        this.contentCategoryService = contentCategoryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminContentCategoryVO>> getContentCategories() {
        log.info("get content category tree request");
        return ApiResponse.success(contentCategoryService.getAdminContentCategoryTree());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createContentCategory(@Valid @RequestBody ContentCategoryCreateRequestDTO requestDTO) {
        log.info("create content category request categoryCode={} parentId={}",
                requestDTO.getCategoryCode(), requestDTO.getParentId());
        contentCategoryService.createContentCategory(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateContentCategory(
            @PathVariable Long id,
            @Valid @RequestBody ContentCategoryUpdateRequestDTO requestDTO) {
        log.info("update content category request id={} version={} categoryCode={} parentId={}",
                id, requestDTO.getVersion(), requestDTO.getCategoryCode(), requestDTO.getParentId());
        contentCategoryService.updateContentCategory(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteContentCategory(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("delete content category request id={} version={}", id, version);
        contentCategoryService.deleteContentCategory(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderContentCategories(@Valid @RequestBody ContentCategoryBatchSortRequestDTO requestDTO) {
        log.info("reorder content categories request count={}",
                requestDTO.getOrderedCategoryIds() == null ? 0 : requestDTO.getOrderedCategoryIds().size());
        contentCategoryService.reorderContentCategories(requestDTO);
        return ApiResponse.success();
    }
}
