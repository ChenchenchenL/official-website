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
import com.company.officialwebsite.modules.media.support.MediaValidationSupport;
import com.company.officialwebsite.modules.media.vo.MediaUploadVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

/**
 * MediaAssetServiceImpl：实现统一媒体上传、引用校验与业务绑定规则。
 */
@Service
public class MediaAssetServiceImpl implements MediaAssetService {

    private static final Logger log = LoggerFactory.getLogger(MediaAssetServiceImpl.class);

    private static final String BIZ_MODULE = "MEDIA";
    private static final String TARGET_TYPE = "MEDIA_ASSET";
    private static final String ACTION_UPLOAD = "UPLOAD_MEDIA";

    private final MediaAssetMapper mediaAssetMapper;
    private final MediaReferenceMapper mediaReferenceMapper;
    private final LocalMediaStorageService localMediaStorageService;
    private final OfficialProperties officialProperties;
    private final MediaValidationSupport mediaValidationSupport;
    private final AuditLogService auditLogService;

    public MediaAssetServiceImpl(
            MediaAssetMapper mediaAssetMapper,
            MediaReferenceMapper mediaReferenceMapper,
            LocalMediaStorageService localMediaStorageService,
            OfficialProperties officialProperties,
            MediaValidationSupport mediaValidationSupport,
            AuditLogService auditLogService) {
        this.mediaAssetMapper = mediaAssetMapper;
        this.mediaReferenceMapper = mediaReferenceMapper;
        this.localMediaStorageService = localMediaStorageService;
        this.officialProperties = officialProperties;
        this.mediaValidationSupport = mediaValidationSupport;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional
    public MediaUploadVO uploadImage(MultipartFile file) {
        return upload(file);
    }

    @Override
    @Transactional
    public MediaUploadVO upload(MultipartFile file) {
        byte[] fileBytes;
        try {
            fileBytes = file == null ? new byte[0] : file.getBytes();
        } catch (IOException ex) {
            log.warn("read upload file bytes failed", ex);
            throw new BusinessException(ErrorCode.MEDIA_UPLOAD_FAILED, "读取上传文件失败");
        }

        String mediaType = mediaValidationSupport.validate(file, fileBytes);

        String originalFilename = file.getOriginalFilename();
        String extension = extractExtension(originalFilename);
        String relativePath;
        try {
            relativePath = localMediaStorageService.storeFile(fileBytes, extension);
        } catch (IOException ex) {
            log.error("store upload file failed", ex);
            throw new BusinessException(ErrorCode.MEDIA_STORAGE_WRITE_FAILED, "文件存储失败");
        }

        MediaAssetEntity entity = new MediaAssetEntity();
        entity.setMediaType(mediaType);
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
            log.error("persist media asset failed, orphan file cleaned path={}", relativePath, ex);
            throw ex;
        }

        log.info("upload media success id={} mediaType={} originalFilename={} size={}",
                entity.getId(), mediaType, originalFilename, entity.getFileSize());
        recordAudit(entity);

        return buildUploadVO(entity);
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

    private MediaUploadVO buildUploadVO(MediaAssetEntity entity) {
        MediaUploadVO vo = new MediaUploadVO();
        vo.setMediaId(entity.getId());
        vo.setMediaType(entity.getMediaType());
        vo.setOriginalFilename(entity.getOriginalFilename());
        vo.setContentType(entity.getContentType());
        vo.setSize(entity.getFileSize());
        vo.setPath(entity.getStoragePath());
        vo.setUrl(entity.getPublicUrl());
        vo.setAbsoluteUrl(buildAbsoluteUrl(entity.getPublicUrl()));
        return vo;
    }

    private String buildPublicUrl(String relativePath) {
        String prefix = officialProperties.getStorage().getPublicUrlPrefix();
        String normalizedPrefix = prefix.endsWith("/") ? prefix : prefix + "/";
        return normalizedPrefix + relativePath;
    }

    private String buildAbsoluteUrl(String publicUrl) {
        String domain = officialProperties.getStorage().getPublicDomain();
        if (!StringUtils.hasText(domain)) {
            return null;
        }
        String normalizedDomain = domain.endsWith("/") ? domain.substring(0, domain.length() - 1) : domain;
        return normalizedDomain + publicUrl;
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID, "文件名缺少扩展名");
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void recordAudit(MediaAssetEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("mediaId", entity.getId());
        snapshot.put("mediaType", entity.getMediaType());
        snapshot.put("originalFilename", entity.getOriginalFilename());
        snapshot.put("contentType", entity.getContentType());
        snapshot.put("fileSize", entity.getFileSize());
        snapshot.put("path", entity.getStoragePath());
        snapshot.put("url", entity.getPublicUrl());
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_UPLOAD, TARGET_TYPE, entity.getId(), null, snapshot);
    }
}
