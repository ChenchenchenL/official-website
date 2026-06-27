package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.PartnerUniversityBatchSortDTO;
import com.company.officialwebsite.modules.site.dto.PartnerUniversityCreateDTO;
import com.company.officialwebsite.modules.site.dto.PartnerUniversityUpdateDTO;
import com.company.officialwebsite.modules.site.service.PartnerUniversityService;
import com.company.officialwebsite.modules.site.vo.AdminPartnerUniversityVO;
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
 * AdminPartnerUniversityController：后台合作高校管理接口。
 */
@RestController
@RequestMapping("/admin/api/partner-universities")
public class AdminPartnerUniversityController {

    private static final Logger log = LoggerFactory.getLogger(AdminPartnerUniversityController.class);

    private final PartnerUniversityService partnerUniversityService;

    public AdminPartnerUniversityController(PartnerUniversityService partnerUniversityService) {
        this.partnerUniversityService = partnerUniversityService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminPartnerUniversityVO>> getUniversities() {
        return ApiResponse.success(partnerUniversityService.getAdminUniversities());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminPartnerUniversityVO>> createUniversity(@Valid @RequestBody PartnerUniversityCreateDTO requestDTO) {
        log.info("create partner university request name={} visible={}", requestDTO.getName(), requestDTO.getVisible());
        return ApiResponse.success(partnerUniversityService.createUniversity(requestDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminPartnerUniversityVO>> updateUniversity(
            @PathVariable Long id,
            @Valid @RequestBody PartnerUniversityUpdateDTO requestDTO) {
        log.info("update partner university request universityId={} version={} visible={}",
                id, requestDTO.getVersion(), requestDTO.getVisible());
        return ApiResponse.success(partnerUniversityService.updateUniversity(id, requestDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminPartnerUniversityVO>> deleteUniversity(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        log.info("delete partner university request universityId={} version={}", id, version);
        return ApiResponse.success(partnerUniversityService.deleteUniversity(id, version));
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminPartnerUniversityVO>> reorderUniversities(
            @Valid @RequestBody PartnerUniversityBatchSortDTO requestDTO) {
        log.info("reorder partner universities request count={}", requestDTO.getOrderedIds() == null ? 0 : requestDTO.getOrderedIds().size());
        return ApiResponse.success(partnerUniversityService.batchSortUniversities(requestDTO));
    }
}
