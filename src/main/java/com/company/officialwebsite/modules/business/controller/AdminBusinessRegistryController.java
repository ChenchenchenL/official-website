package com.company.officialwebsite.modules.business.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryUpdateRequestDTO;
import com.company.officialwebsite.modules.business.service.BusinessRegistryService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessRegistryVO;
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

@Validated
@RestController
@RequestMapping("/admin/api/business-registry")
public class AdminBusinessRegistryController {

    private static final Logger log = LoggerFactory.getLogger(AdminBusinessRegistryController.class);

    private final BusinessRegistryService businessRegistryService;

    public AdminBusinessRegistryController(BusinessRegistryService businessRegistryService) {
        this.businessRegistryService = businessRegistryService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminBusinessRegistryVO>> getBusinessRegistry(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get business registry request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(businessRegistryService.getAdminBusinessRegistryList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createBusinessRegistry(@Valid @RequestBody BusinessRegistryCreateRequestDTO requestDTO) {
        log.info("create business registry request businessCode={}", requestDTO.getBusinessCode());
        businessRegistryService.createBusinessRegistry(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateBusinessRegistry(
            @PathVariable Long id,
            @Valid @RequestBody BusinessRegistryUpdateRequestDTO requestDTO) {
        log.info("update business registry request id={} version={} businessCode={}",
                id, requestDTO.getVersion(), requestDTO.getBusinessCode());
        businessRegistryService.updateBusinessRegistry(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteBusinessRegistry(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("delete business registry request id={} version={}", id, version);
        businessRegistryService.deleteBusinessRegistry(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderBusinessRegistry(@Valid @RequestBody BusinessRegistryBatchSortRequestDTO requestDTO) {
        log.info("reorder business registry request count={}",
                requestDTO.getOrderedBusinessIds() == null ? 0 : requestDTO.getOrderedBusinessIds().size());
        businessRegistryService.reorderBusinessRegistry(requestDTO);
        return ApiResponse.success();
    }
}
