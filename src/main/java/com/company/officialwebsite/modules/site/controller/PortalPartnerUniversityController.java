package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.PartnerUniversityService;
import com.company.officialwebsite.modules.site.vo.PortalPartnerUniversityVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalPartnerUniversityController：前台合作高校展示接口。
 */
@RestController
@RequestMapping("/portal/api/partner-universities")
public class PortalPartnerUniversityController {

    private final PartnerUniversityService partnerUniversityService;

    public PortalPartnerUniversityController(PartnerUniversityService partnerUniversityService) {
        this.partnerUniversityService = partnerUniversityService;
    }

    @GetMapping
    public ApiResponse<List<PortalPartnerUniversityVO>> getUniversities() {
        return ApiResponse.success(partnerUniversityService.getPortalUniversities());
    }
}
