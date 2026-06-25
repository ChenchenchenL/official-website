package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.CapabilityCategoryService;
import com.company.officialwebsite.modules.site.vo.PortalCapabilityCategoryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * PortalCapabilityController：前台公共获取能力底座数据接口。
 */
@RestController
@RequestMapping("/portal/api/site/capabilities")
public class PortalCapabilityController {

    private final CapabilityCategoryService capabilityCategoryService;

    public PortalCapabilityController(CapabilityCategoryService capabilityCategoryService) {
        this.capabilityCategoryService = capabilityCategoryService;
    }

    @GetMapping
    public ApiResponse<List<PortalCapabilityCategoryVO>> getPortalCapabilities() {
        return ApiResponse.success(capabilityCategoryService.getPortalCapabilities());
    }
}
