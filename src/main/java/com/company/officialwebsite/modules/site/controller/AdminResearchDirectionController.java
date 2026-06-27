package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.ResearchDirectionBatchSortDTO;
import com.company.officialwebsite.modules.site.dto.ResearchDirectionCreateDTO;
import com.company.officialwebsite.modules.site.dto.ResearchDirectionUpdateDTO;
import com.company.officialwebsite.modules.site.service.ResearchDirectionService;
import com.company.officialwebsite.modules.site.vo.AdminResearchDirectionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * AdminResearchDirectionController：后台重点研发方向管理接口。
 */
@RestController
@RequestMapping("/admin/api/research-directions")
public class AdminResearchDirectionController {

    private static final Logger log = LoggerFactory.getLogger(AdminResearchDirectionController.class);

    private final ResearchDirectionService researchDirectionService;

    public AdminResearchDirectionController(ResearchDirectionService researchDirectionService) {
        this.researchDirectionService = researchDirectionService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminResearchDirectionVO>> getDirections() {
        return ApiResponse.success(researchDirectionService.getAdminDirections());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminResearchDirectionVO>> createDirection(@Valid @RequestBody ResearchDirectionCreateDTO requestDTO) {
        log.info("create research direction request titleCn={} visible={}", requestDTO.getTitleCn(), requestDTO.getVisible());
        return ApiResponse.success(researchDirectionService.createDirection(requestDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminResearchDirectionVO>> updateDirection(
            @PathVariable Long id,
            @Valid @RequestBody ResearchDirectionUpdateDTO requestDTO) {
        log.info("update research direction request directionId={} version={} visible={}",
                id, requestDTO.getVersion(), requestDTO.getVisible());
        return ApiResponse.success(researchDirectionService.updateDirection(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminResearchDirectionVO>> deleteDirection(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        log.info("delete research direction request directionId={} version={}", id, version);
        return ApiResponse.success(researchDirectionService.deleteDirection(id, version));
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminResearchDirectionVO>> reorderDirections(
            @Valid @RequestBody ResearchDirectionBatchSortDTO requestDTO) {
        log.info("reorder research directions request count={}",
                requestDTO.getOrderedIds() == null ? 0 : requestDTO.getOrderedIds().size());
        return ApiResponse.success(researchDirectionService.batchSortDirections(requestDTO));
    }
}
