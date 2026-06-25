package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.ClientLogoService;
import com.company.officialwebsite.modules.site.vo.PortalClientLogoVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalClientLogoController：提供前台公开服务客户 Logo 墙接口。
 */
@RestController
@RequestMapping("/portal/api/site/client-logos")
public class PortalClientLogoController {

    private final ClientLogoService clientLogoService;

    public PortalClientLogoController(ClientLogoService clientLogoService) {
        this.clientLogoService = clientLogoService;
    }

    @GetMapping
    public ApiResponse<List<PortalClientLogoVO>> getPortalClientLogos() {
        return ApiResponse.success(clientLogoService.getPortalClientLogos());
    }
}
