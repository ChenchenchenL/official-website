package com.company.officialwebsite.modules.casecenter.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.casecenter.dto.CaseBatchSortDTO;
import com.company.officialwebsite.modules.casecenter.dto.CaseCreateDTO;
import com.company.officialwebsite.modules.casecenter.dto.CaseDeleteDTO;
import com.company.officialwebsite.modules.casecenter.dto.CaseUpdateDTO;
import com.company.officialwebsite.modules.casecenter.service.CaseService;
import com.company.officialwebsite.modules.casecenter.vo.AdminCaseVO;
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
 * AdminCaseController：后台标杆案例管理接口。
 */
@RestController
@RequestMapping("/admin/api/cases")
public class AdminCaseController {

    private final CaseService caseService;

    public AdminCaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminCaseVO>> getCaseList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(caseService.getAdminCaseList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminCaseVO>> createCase(@Valid @RequestBody CaseCreateDTO createDTO) {
        return ApiResponse.success(caseService.createCase(createDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminCaseVO>> updateCase(
            @PathVariable Long id,
            @Valid @RequestBody CaseUpdateDTO updateDTO) {
        return ApiResponse.success(caseService.updateCase(id, updateDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminCaseVO>> deleteCase(
            @PathVariable Long id,
            @Valid @RequestBody CaseDeleteDTO deleteDTO) {
        return ApiResponse.success(caseService.deleteCase(id, deleteDTO));
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminCaseVO>> batchSortCases(@Valid @RequestBody CaseBatchSortDTO sortDTO) {
        return ApiResponse.success(caseService.batchSortCases(sortDTO));
    }
}
