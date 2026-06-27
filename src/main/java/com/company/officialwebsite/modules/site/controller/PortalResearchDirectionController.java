package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.ResearchDirectionService;
import com.company.officialwebsite.modules.site.vo.PortalResearchDirectionVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalResearchDirectionController：前台重点研发方向展示接口。
 */
@RestController
@RequestMapping("/portal/api/research-directions")
public class PortalResearchDirectionController {

    private final ResearchDirectionService researchDirectionService;

    public PortalResearchDirectionController(ResearchDirectionService researchDirectionService) {
        this.researchDirectionService = researchDirectionService;
    }

    @GetMapping
    public ApiResponse<List<PortalResearchDirectionVO>> getDirections() {
        return ApiResponse.success(researchDirectionService.getPortalDirections());
    }
}
