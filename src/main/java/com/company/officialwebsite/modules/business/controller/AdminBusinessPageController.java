package com.company.officialwebsite.modules.business.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessPageBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageUpdateRequestDTO;
import com.company.officialwebsite.modules.business.service.BusinessPageService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessPageVO;
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
@RequestMapping("/admin/api/business-pages")
public class AdminBusinessPageController {

    private static final Logger log = LoggerFactory.getLogger(AdminBusinessPageController.class);

    private final BusinessPageService businessPageService;

    public AdminBusinessPageController(BusinessPageService businessPageService) {
        this.businessPageService = businessPageService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminBusinessPageVO>> getBusinessPages(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get business pages request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(businessPageService.getAdminBusinessPageList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createBusinessPage(@Valid @RequestBody BusinessPageCreateRequestDTO requestDTO) {
        log.info("create business page request pageCode={} businessId={}", requestDTO.getPageCode(), requestDTO.getBusinessId());
        businessPageService.createBusinessPage(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateBusinessPage(
            @PathVariable Long id,
            @Valid @RequestBody BusinessPageUpdateRequestDTO requestDTO) {
        log.info("update business page request id={} version={} pageCode={}",
                id, requestDTO.getVersion(), requestDTO.getPageCode());
        businessPageService.updateBusinessPage(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteBusinessPage(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("delete business page request id={} version={}", id, version);
        businessPageService.deleteBusinessPage(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderBusinessPages(@Valid @RequestBody BusinessPageBatchSortRequestDTO requestDTO) {
        log.info("reorder business pages request count={}",
                requestDTO.getOrderedPageIds() == null ? 0 : requestDTO.getOrderedPageIds().size());
        businessPageService.reorderBusinessPages(requestDTO);
        return ApiResponse.success();
    }
}
