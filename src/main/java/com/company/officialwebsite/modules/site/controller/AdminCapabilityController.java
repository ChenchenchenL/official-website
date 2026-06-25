package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.*;
import com.company.officialwebsite.modules.site.service.CapabilityCategoryService;
import com.company.officialwebsite.modules.site.service.CapabilityItemService;
import com.company.officialwebsite.modules.site.vo.CapabilityCategoryVO;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AdminCapabilityController：后台能力底座分类与子项管理接口适配器。
 */
@RestController
@RequestMapping("/admin/api/site")
public class AdminCapabilityController {

    private final CapabilityCategoryService capabilityCategoryService;
    private final CapabilityItemService capabilityItemService;

    public AdminCapabilityController(
            CapabilityCategoryService capabilityCategoryService,
            CapabilityItemService capabilityItemService) {
        this.capabilityCategoryService = capabilityCategoryService;
        this.capabilityItemService = capabilityItemService;
    }

    // --- 分类管理 (Category CRUD) ---

    @GetMapping("/capability-categories")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<CapabilityCategoryVO>> getAdminCategoryTree() {
        return ApiResponse.success(capabilityCategoryService.getAdminCategoryTree());
    }

    @PostMapping("/capability-categories")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Long> createCategory(@Valid @RequestBody CapabilityCategoryCreateDTO requestDTO) {
        return ApiResponse.success(capabilityCategoryService.createCategory(requestDTO));
    }

    @PutMapping("/capability-categories/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CapabilityCategoryUpdateDTO requestDTO) {
        capabilityCategoryService.updateCategory(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/capability-categories/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteCategory(
            @PathVariable Long id,
            @RequestParam("version") Integer version) {
        capabilityCategoryService.deleteCategory(id, version);
        return ApiResponse.success();
    }

    @PutMapping("/capability-categories/batch-sort")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> batchSortCategories(
            @Valid @RequestBody List<@Valid CapabilityCategorySortItemDTO> requestDTO) {
        capabilityCategoryService.batchSortCategories(requestDTO);
        return ApiResponse.success();
    }

    // --- 子项管理 (Capability Item CRUD) ---

    @PostMapping("/capability-items")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Long> createItem(@Valid @RequestBody CapabilityItemCreateDTO requestDTO) {
        return ApiResponse.success(capabilityItemService.createItem(requestDTO));
    }

    @PutMapping("/capability-items/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateItem(
            @PathVariable Long id,
            @Valid @RequestBody CapabilityItemUpdateDTO requestDTO) {
        capabilityItemService.updateItem(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/capability-items/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteItem(
            @PathVariable Long id,
            @RequestParam("version") Integer version) {
        capabilityItemService.deleteItem(id, version);
        return ApiResponse.success();
    }

    @PutMapping("/capability-items/batch-sort")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> batchSortItems(
            @Valid @RequestBody List<@Valid CapabilityItemSortItemDTO> requestDTO) {
        capabilityItemService.batchSortItems(requestDTO);
        return ApiResponse.success();
    }
}
