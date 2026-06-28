package com.company.officialwebsite.modules.lead.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.lead.dto.CooperationDirectionTagBatchSortRequestDTO;
import com.company.officialwebsite.modules.lead.dto.CooperationDirectionTagCreateRequestDTO;
import com.company.officialwebsite.modules.lead.dto.CooperationDirectionTagUpdateRequestDTO;
import com.company.officialwebsite.modules.lead.service.CooperationDirectionTagService;
import com.company.officialwebsite.modules.lead.vo.AdminCooperationDirectionTagVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
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
 * AdminCooperationDirectionTagController：提供后台合作方向标签管理接口。
 */
@Validated
@RestController
@RequestMapping("/admin/api/cooperation-direction-tags")
public class AdminCooperationDirectionTagController {

    private static final Logger log = LoggerFactory.getLogger(AdminCooperationDirectionTagController.class);

    private final CooperationDirectionTagService cooperationDirectionTagService;

    public AdminCooperationDirectionTagController(CooperationDirectionTagService cooperationDirectionTagService) {
        this.cooperationDirectionTagService = cooperationDirectionTagService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminCooperationDirectionTagVO>> getCooperationDirectionTags() {
        log.info("get cooperation direction tags request");
        return ApiResponse.success(cooperationDirectionTagService.getAdminCooperationDirectionTagList());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createCooperationDirectionTag(
            @Valid @RequestBody CooperationDirectionTagCreateRequestDTO requestDTO) {
        log.info("create cooperation direction tag request tagText={}", requestDTO.getTagText());
        cooperationDirectionTagService.createCooperationDirectionTag(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateCooperationDirectionTag(
            @PathVariable Long id,
            @Valid @RequestBody CooperationDirectionTagUpdateRequestDTO requestDTO) {
        log.info("update cooperation direction tag request id={} version={} tagText={}",
                id, requestDTO.getVersion(), requestDTO.getTagText());
        cooperationDirectionTagService.updateCooperationDirectionTag(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteCooperationDirectionTag(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        log.info("delete cooperation direction tag request id={} version={}", id, version);
        cooperationDirectionTagService.deleteCooperationDirectionTag(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderCooperationDirectionTags(
            @Valid @RequestBody CooperationDirectionTagBatchSortRequestDTO requestDTO) {
        log.info("reorder cooperation direction tags request count={}",
                requestDTO.getOrderedCooperationDirectionTagIds() == null
                        ? 0 : requestDTO.getOrderedCooperationDirectionTagIds().size());
        cooperationDirectionTagService.reorderCooperationDirectionTags(requestDTO);
        return ApiResponse.success();
    }
}
