package com.company.officialwebsite.modules.business.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessPageBlockBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageBlockCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageBlockUpdateRequestDTO;
import com.company.officialwebsite.modules.business.service.BusinessPageBlockService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessPageBlockVO;
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
@RequestMapping("/admin/api/business-page-blocks")
public class AdminBusinessPageBlockController {

    private static final Logger log = LoggerFactory.getLogger(AdminBusinessPageBlockController.class);

    private final BusinessPageBlockService businessPageBlockService;

    public AdminBusinessPageBlockController(BusinessPageBlockService businessPageBlockService) {
        this.businessPageBlockService = businessPageBlockService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminBusinessPageBlockVO>> getBusinessPageBlocks(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get business page blocks request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(businessPageBlockService.getAdminBusinessPageBlockList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createBusinessPageBlock(@Valid @RequestBody BusinessPageBlockCreateRequestDTO requestDTO) {
        log.info("create business page block request pageId={} blockId={}", requestDTO.getPageId(), requestDTO.getBlockId());
        businessPageBlockService.createBusinessPageBlock(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateBusinessPageBlock(
            @PathVariable Long id,
            @Valid @RequestBody BusinessPageBlockUpdateRequestDTO requestDTO) {
        log.info("update business page block request id={} version={} pageId={} blockId={}",
                id, requestDTO.getVersion(), requestDTO.getPageId(), requestDTO.getBlockId());
        businessPageBlockService.updateBusinessPageBlock(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteBusinessPageBlock(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("delete business page block request id={} version={}", id, version);
        businessPageBlockService.deleteBusinessPageBlock(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderBusinessPageBlocks(@Valid @RequestBody BusinessPageBlockBatchSortRequestDTO requestDTO) {
        log.info("reorder business page blocks request count={}",
                requestDTO.getOrderedPageBlockIds() == null ? 0 : requestDTO.getOrderedPageBlockIds().size());
        businessPageBlockService.reorderBusinessPageBlocks(requestDTO);
        return ApiResponse.success();
    }
}
