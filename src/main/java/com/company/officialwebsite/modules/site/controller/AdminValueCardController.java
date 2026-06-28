package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.ValueCardBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.ValueCardCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.ValueCardUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.ValueCardService;
import com.company.officialwebsite.modules.site.vo.AdminValueCardVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
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
 * AdminValueCardController：提供后台核心价值观卡片管理接口。
 */
@Validated
@RestController
@RequestMapping("/admin/api/value-cards")
public class AdminValueCardController {

    private static final Logger log = LoggerFactory.getLogger(AdminValueCardController.class);

    private final ValueCardService valueCardService;

    public AdminValueCardController(ValueCardService valueCardService) {
        this.valueCardService = valueCardService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminValueCardVO>> getValueCards(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get value cards request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(valueCardService.getAdminValueCardList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createValueCard(@Valid @RequestBody ValueCardCreateRequestDTO requestDTO) {
        log.info("create value card request title={} visible={}", requestDTO.getTitle(), requestDTO.getVisible());
        valueCardService.createValueCard(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateValueCard(
            @PathVariable Long id,
            @Valid @RequestBody ValueCardUpdateRequestDTO requestDTO) {
        log.info("update value card request id={} version={} title={} visible={}",
                id, requestDTO.getVersion(), requestDTO.getTitle(), requestDTO.getVisible());
        valueCardService.updateValueCard(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteValueCard(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        log.info("delete value card request id={} version={}", id, version);
        valueCardService.deleteValueCard(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderValueCards(@Valid @RequestBody ValueCardBatchSortRequestDTO requestDTO) {
        log.info("reorder value cards request count={}",
                requestDTO.getOrderedValueCardIds() == null ? 0 : requestDTO.getOrderedValueCardIds().size());
        valueCardService.reorderValueCards(requestDTO);
        return ApiResponse.success();
    }
}
