package com.company.officialwebsite.modules.business.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessBlockBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessBlockCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessBlockUpdateRequestDTO;
import com.company.officialwebsite.modules.business.service.BusinessBlockService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessBlockVO;
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
@RequestMapping("/admin/api/business-blocks")
public class AdminBusinessBlockController {

    private static final Logger log = LoggerFactory.getLogger(AdminBusinessBlockController.class);

    private final BusinessBlockService businessBlockService;

    public AdminBusinessBlockController(BusinessBlockService businessBlockService) {
        this.businessBlockService = businessBlockService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminBusinessBlockVO>> getBusinessBlocks(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get business blocks request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(businessBlockService.getAdminBusinessBlockList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createBusinessBlock(@Valid @RequestBody BusinessBlockCreateRequestDTO requestDTO) {
        log.info("create business block request blockCode={}", requestDTO.getBlockCode());
        businessBlockService.createBusinessBlock(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateBusinessBlock(
            @PathVariable Long id,
            @Valid @RequestBody BusinessBlockUpdateRequestDTO requestDTO) {
        log.info("update business block request id={} version={} blockCode={}",
                id, requestDTO.getVersion(), requestDTO.getBlockCode());
        businessBlockService.updateBusinessBlock(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteBusinessBlock(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("delete business block request id={} version={}", id, version);
        businessBlockService.deleteBusinessBlock(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderBusinessBlocks(@Valid @RequestBody BusinessBlockBatchSortRequestDTO requestDTO) {
        log.info("reorder business blocks request count={}",
                requestDTO.getOrderedBlockIds() == null ? 0 : requestDTO.getOrderedBlockIds().size());
        businessBlockService.reorderBusinessBlocks(requestDTO);
        return ApiResponse.success();
    }
}
