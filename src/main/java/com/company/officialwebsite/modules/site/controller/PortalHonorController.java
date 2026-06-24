package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.HonorService;
import com.company.officialwebsite.modules.site.vo.PortalHonorVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalHonorController：提供前台公开荣誉标签接口。
 */
@RestController
@RequestMapping("/portal/api/site/honors")
public class PortalHonorController {

    private final HonorService honorService;

    public PortalHonorController(HonorService honorService) {
        this.honorService = honorService;
    }

    @GetMapping
    public ApiResponse<List<PortalHonorVO>> getHonors() {
        return ApiResponse.success(honorService.getPortalHonors());
    }
}
