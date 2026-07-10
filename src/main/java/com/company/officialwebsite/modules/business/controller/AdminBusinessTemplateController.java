package com.company.officialwebsite.modules.business.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateCreateBusinessRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateUpdateRequestDTO;
import com.company.officialwebsite.modules.business.service.BusinessTemplateService;
import com.company.officialwebsite.modules.business.vo.AdminBusinessTemplateVO;
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
@RequestMapping("/admin/api/business-templates")
public class AdminBusinessTemplateController {

    private static final Logger log = LoggerFactory.getLogger(AdminBusinessTemplateController.class);

    private final BusinessTemplateService businessTemplateService;

    public AdminBusinessTemplateController(BusinessTemplateService businessTemplateService) {
        this.businessTemplateService = businessTemplateService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminBusinessTemplateVO>> getBusinessTemplates(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get business templates request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(businessTemplateService.getAdminBusinessTemplateList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createBusinessTemplate(@Valid @RequestBody BusinessTemplateCreateRequestDTO requestDTO) {
        log.info("create business template request templateCode={} templateType={}",
                requestDTO.getTemplateCode(), requestDTO.getTemplateType());
        businessTemplateService.createBusinessTemplate(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateBusinessTemplate(
            @PathVariable Long id,
            @Valid @RequestBody BusinessTemplateUpdateRequestDTO requestDTO) {
        log.info("update business template request id={} version={} templateCode={}",
                id, requestDTO.getVersion(), requestDTO.getTemplateCode());
        businessTemplateService.updateBusinessTemplate(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteBusinessTemplate(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("delete business template request id={} version={}", id, version);
        businessTemplateService.deleteBusinessTemplate(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/copy")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> copyBusinessTemplate(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("copy business template request id={} version={}", id, version);
        businessTemplateService.copyBusinessTemplate(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/create-business")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createBusinessFromTemplate(
            @PathVariable Long id,
            @Valid @RequestBody BusinessTemplateCreateBusinessRequestDTO requestDTO) {
        log.info("create business from template request id={} businessCode={}", id, requestDTO.getBusinessCode());
        businessTemplateService.createBusinessFromTemplate(id, requestDTO);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderBusinessTemplates(@Valid @RequestBody BusinessTemplateBatchSortRequestDTO requestDTO) {
        log.info("reorder business templates request count={}",
                requestDTO.getOrderedTemplateIds() == null ? 0 : requestDTO.getOrderedTemplateIds().size());
        businessTemplateService.reorderBusinessTemplates(requestDTO);
        return ApiResponse.success();
    }
}
