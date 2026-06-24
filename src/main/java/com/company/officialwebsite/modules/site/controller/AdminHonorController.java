package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.HonorBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.HonorCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.HonorDeleteRequestDTO;
import com.company.officialwebsite.modules.site.dto.HonorUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.HonorService;
import com.company.officialwebsite.modules.site.vo.AdminHonorVO;
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

/**
 * AdminHonorController：提供后台荣誉标签管理接口。
 */
@RestController
@RequestMapping("/admin/api/site/honors")
public class AdminHonorController {

    private final HonorService honorService;

    public AdminHonorController(HonorService honorService) {
        this.honorService = honorService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHonorVO>> getHonors() {
        return ApiResponse.success(honorService.getAdminHonors());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHonorVO>> createHonor(@Valid @RequestBody HonorCreateRequestDTO requestDTO) {
        return ApiResponse.success(honorService.createHonor(requestDTO));
    }

    @PutMapping("/{honorId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHonorVO>> updateHonor(
            @PathVariable Long honorId,
            @Valid @RequestBody HonorUpdateRequestDTO requestDTO) {
        return ApiResponse.success(honorService.updateHonor(honorId, requestDTO));
    }

    @DeleteMapping("/{honorId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHonorVO>> deleteHonor(
            @PathVariable Long honorId,
            @RequestParam("version") Integer version) {
        HonorDeleteRequestDTO requestDTO = new HonorDeleteRequestDTO();
        requestDTO.setVersion(version);
        return ApiResponse.success(honorService.deleteHonor(honorId, requestDTO));
    }

    @PutMapping("/batch-sort")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminHonorVO>> reorderHonors(@Valid @RequestBody HonorBatchSortRequestDTO requestDTO) {
        return ApiResponse.success(honorService.reorderHonors(requestDTO));
    }
}
