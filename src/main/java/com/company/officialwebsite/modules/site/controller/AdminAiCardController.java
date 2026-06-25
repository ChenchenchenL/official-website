package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.AiCardCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.AiCardSortItemDTO;
import com.company.officialwebsite.modules.site.dto.AiCardUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.AiCardService;
import com.company.officialwebsite.modules.site.vo.AdminAiCardVO;
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
 * AdminAiCardController：后台 AI 战略模块化卡片管理接口。
 */
@RestController
@RequestMapping("/admin/api/site/ai-cards")
public class AdminAiCardController {

    private final AiCardService aiCardService;

    public AdminAiCardController(AiCardService aiCardService) {
        this.aiCardService = aiCardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminAiCardVO>> getAiCardList(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return ApiResponse.success(aiCardService.getAdminCards(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Long> createAiCard(@Valid @RequestBody AiCardCreateRequestDTO requestDTO) {
        return ApiResponse.success(aiCardService.createCard(requestDTO));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateAiCard(
            @PathVariable Long id,
            @Valid @RequestBody AiCardUpdateRequestDTO requestDTO) {
        aiCardService.updateCard(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteAiCard(
            @PathVariable Long id,
            @RequestParam("version") Integer version) {
        aiCardService.deleteCard(id, version);
        return ApiResponse.success();
    }

    @PutMapping("/batch-sort")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> batchSortAiCards(@Valid @RequestBody List<@Valid AiCardSortItemDTO> requestDTO) {
        aiCardService.batchSortCards(requestDTO);
        return ApiResponse.success();
    }
}
