package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.ValueCardService;
import com.company.officialwebsite.modules.site.vo.PortalValueCardVO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalValueCardController：提供前台公开核心价值观卡片接口。
 */
@RestController
@RequestMapping("/portal/api/value-cards")
public class PortalValueCardController {

    private static final Logger log = LoggerFactory.getLogger(PortalValueCardController.class);

    private final ValueCardService valueCardService;

    public PortalValueCardController(ValueCardService valueCardService) {
        this.valueCardService = valueCardService;
    }

    @GetMapping
    public ApiResponse<List<PortalValueCardVO>> getValueCards() {
        log.info("get portal value cards request");
        return ApiResponse.success(valueCardService.getPortalValueCards());
    }
}
