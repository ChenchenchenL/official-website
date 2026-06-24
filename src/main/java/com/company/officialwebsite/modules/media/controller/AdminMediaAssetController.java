package com.company.officialwebsite.modules.media.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.media.vo.MediaUploadVO;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
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
     * 上传站点配置等后台模块可复用的公共图片资源。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<MediaUploadVO> uploadImage(@RequestPart("file") MultipartFile file) {
        return ApiResponse.success(mediaAssetService.uploadImage(file));
    }
}
