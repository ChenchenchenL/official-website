package com.company.officialwebsite.modules.lead.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.infrastructure.security.AdminUserPrincipal;
import com.company.officialwebsite.modules.lead.dto.LeadExportRequestDTO;
import com.company.officialwebsite.modules.lead.dto.LeadQueryRequestDTO;
import com.company.officialwebsite.modules.lead.dto.LeadStatusUpdateRequestDTO;
import com.company.officialwebsite.modules.lead.service.LeadService;
import com.company.officialwebsite.modules.lead.vo.AdminLeadDetailVO;
import com.company.officialwebsite.modules.lead.vo.AdminLeadVO;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminLeadController：提供后台线索分页查询、详情查看、状态流转和 Excel 导出。
 */
@Validated
@RestController
@RequestMapping("/admin/api/leads")
public class AdminLeadController {

    private static final Logger log = LoggerFactory.getLogger(AdminLeadController.class);

    private final LeadService leadService;

    public AdminLeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminLeadVO>> getLeadPage(@Valid LeadQueryRequestDTO requestDTO) {
        log.info("get lead page request pageNo={} pageSize={} status={}",
                requestDTO.getPageNo(), requestDTO.getPageSize(), requestDTO.getStatus());
        return ApiResponse.success(leadService.getAdminLeadPage(requestDTO));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminLeadDetailVO> getLeadDetail(@PathVariable Long id) {
        log.info("get lead detail request id={}", id);
        return ApiResponse.success(leadService.getAdminLeadDetail(id));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateLeadStatus(
            @PathVariable Long id,
            @Valid @RequestBody LeadStatusUpdateRequestDTO requestDTO,
            @AuthenticationPrincipal AdminUserPrincipal principal) {
        log.info("update lead status request id={} status={}", id, requestDTO.getStatus());
        leadService.updateLeadStatus(id, requestDTO, principal.getUserId());
        return ApiResponse.success();
    }

    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public void exportLeads(
            @Valid @RequestBody LeadExportRequestDTO requestDTO,
            HttpServletResponse response,
            @AuthenticationPrincipal AdminUserPrincipal principal) {
        log.info("export leads request mode={}", requestDTO.getExportMode());
        leadService.exportLeads(requestDTO, response, principal.getUserId());
    }
}
