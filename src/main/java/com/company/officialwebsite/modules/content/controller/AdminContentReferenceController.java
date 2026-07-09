package com.company.officialwebsite.modules.content.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.content.dto.ContentReferenceCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentReferenceUpdateRequestDTO;
import com.company.officialwebsite.modules.content.service.ContentReferenceService;
import com.company.officialwebsite.modules.content.vo.AdminContentReferenceVO;
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
@RequestMapping("/admin/api/content-references")
public class AdminContentReferenceController {

    private static final Logger log = LoggerFactory.getLogger(AdminContentReferenceController.class);

    private final ContentReferenceService contentReferenceService;

    public AdminContentReferenceController(ContentReferenceService contentReferenceService) {
        this.contentReferenceService = contentReferenceService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminContentReferenceVO>> getContentReferences(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get content references request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(contentReferenceService.getAdminContentReferenceList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createContentReference(@Valid @RequestBody ContentReferenceCreateRequestDTO requestDTO) {
        log.info("create content reference request referrerType={} referrerKey={} referencedType={} referencedId={} referenceType={}",
                requestDTO.getReferrerType(), requestDTO.getReferrerKey(), requestDTO.getReferencedType(),
                requestDTO.getReferencedId(), requestDTO.getReferenceType());
        contentReferenceService.createContentReference(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateContentReference(
            @PathVariable Long id,
            @Valid @RequestBody ContentReferenceUpdateRequestDTO requestDTO) {
        log.info("update content reference request id={} version={} referrerType={} referrerKey={} referencedType={} referencedId={} referenceType={}",
                id, requestDTO.getVersion(), requestDTO.getReferrerType(), requestDTO.getReferrerKey(),
                requestDTO.getReferencedType(), requestDTO.getReferencedId(), requestDTO.getReferenceType());
        contentReferenceService.updateContentReference(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteContentReference(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("delete content reference request id={} version={}", id, version);
        contentReferenceService.deleteContentReference(id, version);
        return ApiResponse.success();
    }
}
