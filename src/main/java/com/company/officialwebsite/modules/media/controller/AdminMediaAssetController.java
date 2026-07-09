package com.company.officialwebsite.modules.media.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.media.dto.MediaAssetUpdateDTO;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.media.vo.MediaAssetVO;
import com.company.officialwebsite.modules.media.vo.MediaUploadVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * AdminMediaAssetController：提供后台统一媒体上传入口。
 */
@RestController
@RequestMapping("/admin/api/media/assets")
public class AdminMediaAssetController {

    private final MediaAssetService mediaAssetService;

    public AdminMediaAssetController(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    /**
     * 上传站点配置等后台模块可复用的公共图片或文档资源。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<MediaUploadVO> uploadAsset(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(mediaAssetService.upload(file));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<MediaAssetVO>> listAssets(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String mediaType,
            @RequestParam(required = false) String usageTag,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(mediaAssetService.listAssets(keyword, mediaType, usageTag, status, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<MediaAssetVO> getAsset(@PathVariable Long id) {
        return ApiResponse.success(mediaAssetService.getAsset(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<MediaAssetVO> updateAsset(
            @PathVariable Long id,
            @Valid @RequestBody MediaAssetUpdateDTO updateDTO) {
        return ApiResponse.success(mediaAssetService.updateAsset(id, updateDTO));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteAsset(
            @PathVariable Long id,
            @RequestParam("version") Integer version) {
        mediaAssetService.deleteAsset(id, version);
        return ApiResponse.success();
    }
}
