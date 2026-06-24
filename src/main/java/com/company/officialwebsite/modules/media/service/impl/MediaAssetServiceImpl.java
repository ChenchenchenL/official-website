package com.company.officialwebsite.modules.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.infrastructure.storage.LocalMediaStorageService;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.entity.MediaReferenceEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.media.mapper.MediaReferenceMapper;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.media.vo.MediaUploadVO;
import java.io.IOException;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * MediaAssetServiceImpl：实现后台图片上传和媒体引用绑定规则。
 */
@Service
public class MediaAssetServiceImpl implements MediaAssetService {

    private final MediaAssetMapper mediaAssetMapper;
    private final MediaReferenceMapper mediaReferenceMapper;
    private final LocalMediaStorageService localMediaStorageService;
    private final OfficialProperties officialProperties;

    public MediaAssetServiceImpl(
            MediaAssetMapper mediaAssetMapper,
            MediaReferenceMapper mediaReferenceMapper,
            LocalMediaStorageService localMediaStorageService,
            OfficialProperties officialProperties) {
        this.mediaAssetMapper = mediaAssetMapper;
        this.mediaReferenceMapper = mediaReferenceMapper;
        this.localMediaStorageService = localMediaStorageService;
        this.officialProperties = officialProperties;
    }

    @Override
    @Transactional
    public MediaUploadVO uploadImage(MultipartFile file) {
        byte[] fileBytes;
        try {
            fileBytes = file == null ? new byte[0] : file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID);
        }
        validateImage(file, fileBytes);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String relativePath;
        try {
            relativePath = localMediaStorageService.storeImage(fileBytes, extension);
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, ErrorCode.SYSTEM_ERROR.getDefaultMessage(), ex);
        }

        MediaAssetEntity entity = new MediaAssetEntity();
        entity.setMediaType("IMAGE");
        entity.setStatus(MediaAssetStatusEnum.TEMPORARY.getCode());
        entity.setOriginalFilename(originalFilename);
        entity.setContentType(file.getContentType());
        entity.setStoragePath(relativePath);
        entity.setPublicUrl(buildPublicUrl(relativePath));
        entity.setFileSize(file.getSize());

        try {
            mediaAssetMapper.insert(entity);
        } catch (RuntimeException ex) {
            localMediaStorageService.deleteQuietly(relativePath);
            throw ex;
        }

        MediaUploadVO response = new MediaUploadVO();
        response.setMediaId(entity.getId());
        response.setUrl(entity.getPublicUrl());
        response.setContentType(entity.getContentType());
        response.setSize(entity.getFileSize());
        return response;
    }

    @Override
    public MediaAssetEntity requireUsableImage(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        MediaAssetEntity entity = mediaAssetMapper.selectById(mediaId);
        if (entity == null || entity.getDeletedMarker() != null && entity.getDeletedMarker() != 0L) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND);
        }
        if (!"IMAGE".equals(entity.getMediaType())) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND);
        }
        MediaAssetStatusEnum status = MediaAssetStatusEnum.fromCode(entity.getStatus());
        if (status == MediaAssetStatusEnum.DELETED) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND);
        }
        return entity;
    }

    @Override
    @Transactional
    public void bindMedia(Long mediaId, String bizModule, Long bizObjectId, String bizField) {
        MediaReferenceEntity existing = mediaReferenceMapper.selectOne(new LambdaQueryWrapper<MediaReferenceEntity>()
                .eq(MediaReferenceEntity::getBizModule, bizModule)
                .eq(MediaReferenceEntity::getBizObjectId, bizObjectId)
                .eq(MediaReferenceEntity::getBizField, bizField)
                .eq(MediaReferenceEntity::getDeletedMarker, 0L)
                .last("limit 1"));

        Long oldMediaId = existing == null ? null : existing.getMediaId();
        if (mediaId != null) {
            requireUsableImage(mediaId);
        }

        if (mediaId == null) {
            if (existing != null) {
                mediaReferenceMapper.deleteById(existing.getId());
            }
            refreshMediaStatus(oldMediaId);
            return;
        }

        if (existing != null) {
            if (!mediaId.equals(existing.getMediaId())) {
                existing.setMediaId(mediaId);
                mediaReferenceMapper.updateById(existing);
            }
        } else {
            MediaReferenceEntity entity = new MediaReferenceEntity();
            entity.setMediaId(mediaId);
            entity.setBizModule(bizModule);
            entity.setBizObjectId(bizObjectId);
            entity.setBizField(bizField);
            mediaReferenceMapper.insert(entity);
        }

        refreshMediaStatus(mediaId);
        if (oldMediaId != null && !oldMediaId.equals(mediaId)) {
            refreshMediaStatus(oldMediaId);
        }
    }

    private void refreshMediaStatus(Long mediaId) {
        if (mediaId == null) {
            return;
        }
        MediaAssetEntity entity = mediaAssetMapper.selectById(mediaId);
        if (entity == null || entity.getDeletedMarker() == null || entity.getDeletedMarker() != 0L) {
            return;
        }
        if (MediaAssetStatusEnum.DELETED.getCode().equals(entity.getStatus())) {
            return;
        }
        boolean referenced = mediaReferenceMapper.selectCount(new LambdaQueryWrapper<MediaReferenceEntity>()
                .eq(MediaReferenceEntity::getMediaId, mediaId)
                .eq(MediaReferenceEntity::getDeletedMarker, 0L)) > 0;
        entity.setStatus(referenced
                ? MediaAssetStatusEnum.BOUND.getCode()
                : MediaAssetStatusEnum.UNBOUND.getCode());
        mediaAssetMapper.updateById(entity);
    }

    /**
     * 同时校验扩展名、MIME 和内容签名，防止伪装脚本或损坏文件进入媒体库。
     */
    private void validateImage(MultipartFile file, byte[] fileBytes) {
        if (file == null || file.isEmpty() || fileBytes.length == 0) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID);
        }
        if (file.getSize() > officialProperties.getStorage().getMaxImageSizeBytes()) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID, "图片大小超出限制");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.startsWith("image/")) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID);
        }
        String extension = extractExtension(file.getOriginalFilename());
        boolean supportedExtension = "png".equals(extension)
                || "jpg".equals(extension)
                || "jpeg".equals(extension)
                || "webp".equals(extension);
        if (!supportedExtension || !matchesSignature(extension, fileBytes)) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID);
        }
    }

    private boolean matchesSignature(String extension, byte[] fileBytes) {
        return switch (extension) {
            case "png" -> hasPrefix(fileBytes, new int[] {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
            case "jpg", "jpeg" -> hasPrefix(fileBytes, new int[] {0xFF, 0xD8, 0xFF});
            case "webp" -> hasWebpSignature(fileBytes);
            default -> false;
        };
    }

    private boolean hasPrefix(byte[] fileBytes, int[] prefix) {
        if (fileBytes.length < prefix.length) {
            return false;
        }
        for (int index = 0; index < prefix.length; index++) {
            if ((fileBytes[index] & 0xFF) != prefix[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean hasWebpSignature(byte[] fileBytes) {
        if (fileBytes.length < 12) {
            return false;
        }
        return fileBytes[0] == 'R'
                && fileBytes[1] == 'I'
                && fileBytes[2] == 'F'
                && fileBytes[3] == 'F'
                && fileBytes[8] == 'W'
                && fileBytes[9] == 'E'
                && fileBytes[10] == 'B'
                && fileBytes[11] == 'P';
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID);
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String buildPublicUrl(String relativePath) {
        String prefix = officialProperties.getStorage().getPublicUrlPrefix();
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return normalizedPrefix + relativePath;
    }
}
