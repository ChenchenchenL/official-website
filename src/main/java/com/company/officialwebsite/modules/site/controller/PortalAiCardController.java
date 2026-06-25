package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.AiCardService;
import com.company.officialwebsite.modules.site.vo.PortalAiCardVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalAiCardController：公开门户端 AI 战略卡片展示接口。
 */
@RestController
@RequestMapping("/portal/api/site/ai-cards")
public class PortalAiCardController {

    private final AiCardService aiCardService;

    public PortalAiCardController(AiCardService aiCardService) {
        this.aiCardService = aiCardService;
    }

    @GetMapping
    public ApiResponse<List<PortalAiCardVO>> getPortalAiCards() {
        return ApiResponse.success(aiCardService.getPortalCards());
    }
}
