package com.company.officialwebsite.modules.product.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.product.service.IndustrySolutionService;
import com.company.officialwebsite.modules.product.vo.PortalIndustrySolutionVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalIndustrySolutionController：前台行业解决方案公开接口。
 */
@RestController
@RequestMapping("/portal/api/industry-solutions")
public class PortalIndustrySolutionController {

    private final IndustrySolutionService industrySolutionService;

    public PortalIndustrySolutionController(IndustrySolutionService industrySolutionService) {
        this.industrySolutionService = industrySolutionService;
    }

    @GetMapping
    public ApiResponse<List<PortalIndustrySolutionVO>> getPortalIndustrySolutions() {
        return ApiResponse.success(industrySolutionService.getPortalIndustrySolutions());
    }
}
