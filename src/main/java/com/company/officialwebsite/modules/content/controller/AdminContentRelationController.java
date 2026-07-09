package com.company.officialwebsite.modules.content.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.content.dto.ContentRelationCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentRelationUpdateRequestDTO;
import com.company.officialwebsite.modules.content.service.ContentRelationService;
import com.company.officialwebsite.modules.content.vo.AdminContentRelationVO;
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

@Validated
@RestController
@RequestMapping("/admin/api/content-relations")
public class AdminContentRelationController {

    private static final Logger log = LoggerFactory.getLogger(AdminContentRelationController.class);

    private final ContentRelationService contentRelationService;

    public AdminContentRelationController(ContentRelationService contentRelationService) {
        this.contentRelationService = contentRelationService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminContentRelationVO>> getContentRelations(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get content relations request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(contentRelationService.getAdminContentRelationList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createContentRelation(@Valid @RequestBody ContentRelationCreateRequestDTO requestDTO) {
        log.info("create content relation request sourceType={} sourceId={} targetType={} targetId={} relationType={}",
                requestDTO.getSourceType(), requestDTO.getSourceId(), requestDTO.getTargetType(),
                requestDTO.getTargetId(), requestDTO.getRelationType());
        contentRelationService.createContentRelation(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateContentRelation(
            @PathVariable Long id,
            @Valid @RequestBody ContentRelationUpdateRequestDTO requestDTO) {
        log.info("update content relation request id={} version={} sourceType={} sourceId={} targetType={} targetId={} relationType={}",
                id, requestDTO.getVersion(), requestDTO.getSourceType(), requestDTO.getSourceId(),
                requestDTO.getTargetType(), requestDTO.getTargetId(), requestDTO.getRelationType());
        contentRelationService.updateContentRelation(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteContentRelation(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("delete content relation request id={} version={}", id, version);
        contentRelationService.deleteContentRelation(id, version);
        return ApiResponse.success();
    }
}
