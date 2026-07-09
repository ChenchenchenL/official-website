package com.company.officialwebsite.modules.casecenter.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.casecenter.service.CaseService;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseDetailVO;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalCaseController：前台标杆案例公开接口。
 */
@RestController
@RequestMapping("/portal/api/cases")
public class PortalCaseController {

    private final CaseService caseService;

    public PortalCaseController(CaseService caseService) {
        this.caseService = caseService;
    }

    @GetMapping
    public ApiResponse<List<PortalCaseVO>> getPortalCases() {
        return ApiResponse.success(caseService.getPortalCases());
    }

    @GetMapping("/{id}")
    public ApiResponse<PortalCaseDetailVO> getPortalCaseDetail(@PathVariable Long id) {
        return ApiResponse.success(caseService.getPortalCaseDetail(id));
    }
}
