package com.company.officialwebsite.modules.content.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.content.dto.ContentTagBatchSortRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentTagCreateRequestDTO;
import com.company.officialwebsite.modules.content.dto.ContentTagUpdateRequestDTO;
import com.company.officialwebsite.modules.content.service.ContentTagService;
import com.company.officialwebsite.modules.content.vo.AdminContentTagVO;
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
@RequestMapping("/admin/api/content-tags")
public class AdminContentTagController {

    private static final Logger log = LoggerFactory.getLogger(AdminContentTagController.class);

    private final ContentTagService contentTagService;

    public AdminContentTagController(ContentTagService contentTagService) {
        this.contentTagService = contentTagService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminContentTagVO>> getContentTags(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get content tags request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(contentTagService.getAdminContentTagList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createContentTag(@Valid @RequestBody ContentTagCreateRequestDTO requestDTO) {
        log.info("create content tag request tagCode={} tagName={}", requestDTO.getTagCode(), requestDTO.getTagName());
        contentTagService.createContentTag(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateContentTag(
            @PathVariable Long id,
            @Valid @RequestBody ContentTagUpdateRequestDTO requestDTO) {
        log.info("update content tag request id={} version={} tagCode={}",
                id, requestDTO.getVersion(), requestDTO.getTagCode());
        contentTagService.updateContentTag(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteContentTag(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "Version cannot be negative") Integer version) {
        log.info("delete content tag request id={} version={}", id, version);
        contentTagService.deleteContentTag(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderContentTags(@Valid @RequestBody ContentTagBatchSortRequestDTO requestDTO) {
        log.info("reorder content tags request count={}",
                requestDTO.getOrderedTagIds() == null ? 0 : requestDTO.getOrderedTagIds().size());
        contentTagService.reorderContentTags(requestDTO);
        return ApiResponse.success();
    }
}
