package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.PageSectionRequestDTO;
import com.company.officialwebsite.modules.site.dto.PageSectionSortItemDTO;
import com.company.officialwebsite.modules.site.dto.PageSectionStatusDTO;
import com.company.officialwebsite.modules.site.dto.PageSectionVisibilityDTO;
import com.company.officialwebsite.modules.site.service.PageSectionService;
import com.company.officialwebsite.modules.site.vo.PageSectionVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/api/site/page-sections")
public class AdminPageSectionController {

    private final PageSectionService pageSectionService;

    public AdminPageSectionController(PageSectionService pageSectionService) {
        this.pageSectionService = pageSectionService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<PageSectionVO>> listSections(
            @RequestParam(required = false) String pageCode,
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(pageSectionService.getAdminSections(pageCode, pageNo, pageSize));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageSectionVO> getSection(@PathVariable Long id) {
        return ApiResponse.success(pageSectionService.getAdminSection(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Long> createSection(@Valid @RequestBody PageSectionRequestDTO requestDTO) {
        return ApiResponse.success(pageSectionService.createSection(requestDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageSectionVO> updateSection(
            @PathVariable Long id,
            @Valid @RequestBody PageSectionRequestDTO requestDTO) {
        return ApiResponse.success(pageSectionService.updateSection(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteSection(
            @PathVariable Long id,
            @RequestParam("version") Integer version) {
        pageSectionService.deleteSection(id, version);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/visibility")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageSectionVO> updateVisibility(
            @PathVariable Long id,
            @Valid @RequestBody PageSectionVisibilityDTO requestDTO) {
        return ApiResponse.success(pageSectionService.updateVisibility(id, requestDTO));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageSectionVO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody PageSectionStatusDTO requestDTO) {
        return ApiResponse.success(pageSectionService.updateStatus(id, requestDTO));
    }

    @PutMapping("/batch-sort")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> batchSort(@Valid @RequestBody List<@Valid PageSectionSortItemDTO> sortItems) {
        pageSectionService.batchSort(sortItems);
        return ApiResponse.success();
    }
}
