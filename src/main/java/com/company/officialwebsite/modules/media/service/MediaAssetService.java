package com.company.officialwebsite.modules.media.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.media.dto.MediaAssetUpdateDTO;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.vo.MediaAssetVO;
import com.company.officialwebsite.modules.media.vo.MediaUploadVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * MediaAssetService：封装媒体上传、校验和业务绑定能力。
 */
public interface MediaAssetService {

    /**
     * 上传图片并返回可供业务模块引用的媒体信息。
     */
    MediaUploadVO uploadImage(MultipartFile file);

    /**
     * 统一上传入口，支持图片与文档两类公开素材。
     */
    MediaUploadVO upload(MultipartFile file);

    PageResult<MediaAssetVO> listAssets(
            String keyword, String mediaType, String usageTag, String status, Integer page, Integer size);

    MediaAssetVO getAsset(Long id);

    MediaAssetVO updateAsset(Long id, MediaAssetUpdateDTO updateDTO);

    void deleteAsset(Long id, Integer version);

    /**
     * 校验媒体是否为业务可引用的公开图片资源。
     */
    MediaAssetEntity requireUsableImage(Long mediaId);

    /**
     * 维护业务字段到媒体资源的绑定关系，并同步媒体生命周期状态。
     */
    void bindMedia(Long mediaId, String bizModule, Long bizObjectId, String bizField);
}
