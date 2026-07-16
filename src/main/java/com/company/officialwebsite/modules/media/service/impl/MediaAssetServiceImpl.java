package com.company.officialwebsite.modules.media.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.enums.MediaAssetStatusEnum;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.infrastructure.event.EntityChangedEvent;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.infrastructure.storage.LocalMediaStorageService;
import com.company.officialwebsite.modules.content.service.ContentReferenceGuard;
import com.company.officialwebsite.modules.media.dto.MediaAssetUpdateDTO;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.entity.MediaReferenceEntity;
import com.company.officialwebsite.modules.media.mapper.MediaAssetMapper;
import com.company.officialwebsite.modules.media.mapper.MediaReferenceMapper;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.media.support.MediaValidationSupport;
import com.company.officialwebsite.modules.media.vo.MediaAssetVO;
import com.company.officialwebsite.modules.media.vo.MediaUploadVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
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
    private static final String ACTION_UPDATE = "UPDATE_MEDIA";
    private static final String ACTION_DELETE = "DELETE_MEDIA";
    private static final String DEFAULT_USAGE_TAG = "OTHER";
    private static final List<String> ACTIVE_LIKE_STATUSES = List.of("ACTIVE", "TEMPORARY", "BOUND", "UNBOUND");
    private static final List<String> USAGE_TAGS = List.of("LOGO", "BANNER", "PRODUCT", "CASE", "CLIENT", "HONOR", "ICON", "OTHER");
    private static final ObjectMapper SNAPSHOT_OBJECT_MAPPER = new ObjectMapper();

    private final MediaAssetMapper mediaAssetMapper;
    private final MediaReferenceMapper mediaReferenceMapper;
    private final LocalMediaStorageService localMediaStorageService;
    private final OfficialProperties officialProperties;
    private final MediaValidationSupport mediaValidationSupport;
    private final AuditLogService auditLogService;
    private final ApplicationEventPublisher eventPublisher;
    private final ContentReferenceGuard contentReferenceGuard;

    public MediaAssetServiceImpl(
            MediaAssetMapper mediaAssetMapper,
            MediaReferenceMapper mediaReferenceMapper,
            LocalMediaStorageService localMediaStorageService,
            OfficialProperties officialProperties,
            MediaValidationSupport mediaValidationSupport,
            AuditLogService auditLogService,
            ApplicationEventPublisher eventPublisher,
            ContentReferenceGuard contentReferenceGuard) {
        this.mediaAssetMapper = mediaAssetMapper;
        this.mediaReferenceMapper = mediaReferenceMapper;
        this.localMediaStorageService = localMediaStorageService;
        this.officialProperties = officialProperties;
        this.mediaValidationSupport = mediaValidationSupport;
        this.auditLogService = auditLogService;
        this.eventPublisher = eventPublisher;
        this.contentReferenceGuard = contentReferenceGuard;
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
        entity.setStatus(MediaAssetStatusEnum.ACTIVE.getCode());
        entity.setOriginalFilename(originalFilename);
        entity.setContentType(file.getContentType());
        entity.setStoragePath(relativePath);
        entity.setPublicUrl(buildPublicUrl(relativePath));
        entity.setFileSize(file.getSize());
        entity.setUsageTag(DEFAULT_USAGE_TAG);

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
    @Transactional(readOnly = true)
    public PageResult<MediaAssetVO> listAssets(
            String keyword, String mediaType, String usageTag, String status, Integer page, Integer size) {
        int normalizedPageNo = page == null || page <= 0 ? 1 : page;
        int normalizedPageSize = size == null || size <= 0 ? 20 : Math.min(size, 100);

        LambdaQueryWrapper<MediaAssetEntity> wrapper = new LambdaQueryWrapper<MediaAssetEntity>()
                .eq(MediaAssetEntity::getDeletedMarker, 0L);

        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            wrapper.and(query -> query
                    .like(MediaAssetEntity::getOriginalFilename, normalizedKeyword)
                    .or()
                    .like(MediaAssetEntity::getPublicUrl, normalizedKeyword)
                    .or()
                    .like(MediaAssetEntity::getStoragePath, normalizedKeyword)
                    .or()
                    .like(MediaAssetEntity::getAltText, normalizedKeyword)
                    .or()
                    .like(MediaAssetEntity::getRemark, normalizedKeyword));
        }
        if (StringUtils.hasText(mediaType)) {
            String normalizedMediaType = normalizeMediaType(mediaType);
            wrapper.eq(MediaAssetEntity::getMediaType, normalizedMediaType);
        }
        if (StringUtils.hasText(usageTag)) {
            wrapper.eq(MediaAssetEntity::getUsageTag, normalizeUsageTag(usageTag));
        }
        if (StringUtils.hasText(status)) {
            String normalizedStatus = normalizeStatus(status);
            if (MediaAssetStatusEnum.ACTIVE.getCode().equals(normalizedStatus)) {
                wrapper.in(MediaAssetEntity::getStatus, ACTIVE_LIKE_STATUSES);
            } else {
                wrapper.eq(MediaAssetEntity::getStatus, normalizedStatus);
            }
        }

        Page<MediaAssetEntity> pageResult = mediaAssetMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                wrapper.orderByDesc(MediaAssetEntity::getCreatedAt).orderByDesc(MediaAssetEntity::getId));
        List<MediaAssetVO> list = pageResult.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, pageResult.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public MediaAssetVO getAsset(Long id) {
        return toVO(requireAsset(id));
    }

    @Override
    @Transactional
    public MediaAssetVO updateAsset(Long id, MediaAssetUpdateDTO updateDTO) {
        MediaAssetEntity entity = requireAsset(id);
        if (MediaAssetStatusEnum.isDeleted(entity.getStatus())) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "已删除媒体不能编辑");
        }
        ConcurrencyHelper.assertVersion(entity.getVersion(), updateDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);

        entity.setUsageTag(StringUtils.hasText(updateDTO.getUsageTag())
                ? normalizeUsageTag(updateDTO.getUsageTag())
                : DEFAULT_USAGE_TAG);
        entity.setAltText(trimToNull(updateDTO.getAltText()));
        entity.setRemark(trimToNull(updateDTO.getRemark()));
        ConcurrencyHelper.tryUpdate(mediaAssetMapper, entity);

        log.info("update media asset success id={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        return toVO(entity);
    }

    @Override
    @Transactional
    public void deleteAsset(Long id, Integer version) {
        MediaAssetEntity entity = requireAsset(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        if (MediaAssetStatusEnum.isDeleted(entity.getStatus())) {
            return;
        }
        contentReferenceGuard.assertNotReferenced(TARGET_TYPE, entity.getId());
        contentReferenceGuard.assertNotReferencedByPage("media", "MediaAssetEntity", entity.getId());

        boolean referenced = mediaReferenceMapper.selectCount(new LambdaQueryWrapper<MediaReferenceEntity>()
                .eq(MediaReferenceEntity::getMediaId, id)
                .eq(MediaReferenceEntity::getDeletedMarker, 0L)) > 0;
        if (referenced) {
            throw new BusinessException(ErrorCode.RESOURCE_REFERENCE_CONFLICT, "该媒体正在被已发布的页面或数据绑定使用，无法删除");
        }

        Map<String, Object> before = toSnapshot(entity);
        entity.setStatus(MediaAssetStatusEnum.DELETED.getCode());
        ConcurrencyHelper.tryUpdate(mediaAssetMapper, entity);

        log.info("delete media asset success id={}", entity.getId());
        recordAudit(ACTION_DELETE, entity.getId(), before, toSnapshot(entity));
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
        if (MediaAssetStatusEnum.isDeleted(entity.getStatus())) {
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
                refreshMediaStatus(oldMediaId);
                eventPublisher.publishEvent(EntityChangedEvent.of(this, "media", "MediaAsset", String.valueOf(oldMediaId)));
            }
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
            eventPublisher.publishEvent(EntityChangedEvent.of(this, "media", "MediaAsset", String.valueOf(oldMediaId)));
        }
        eventPublisher.publishEvent(EntityChangedEvent.of(this, "media", "MediaAsset", String.valueOf(mediaId)));
    }

    @Override
    @Transactional
    public void bindPublishedSnapshotMedia(String snapshotModule, Long snapshotId, String snapshotJson) {
        if (!StringUtils.hasText(snapshotModule) || snapshotId == null || snapshotId <= 0 || !StringUtils.hasText(snapshotJson)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "发布快照媒体引用参数不完整");
        }
        try {
            Map<String, Long> mediaByField = new LinkedHashMap<>();
            collectSnapshotMediaIds(SNAPSHOT_OBJECT_MAPPER.readTree(snapshotJson), "root", mediaByField);
            for (Map.Entry<String, Long> entry : mediaByField.entrySet()) {
                bindMedia(entry.getValue(), snapshotModule, snapshotId, toSnapshotBizField(entry.getKey()));
            }
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "发布快照 JSON 格式无效", exception);
        }
    }

    private void collectSnapshotMediaIds(JsonNode node, String fieldPath, Map<String, Long> mediaByField) {
        if (node == null || node.isNull()) {
            return;
        }
        if (node.isObject()) {
            node.fields().forEachRemaining(entry -> collectSnapshotMediaIds(
                    entry.getValue(), fieldPath + "." + entry.getKey(), mediaByField));
            return;
        }
        if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                collectSnapshotMediaIds(node.get(index), fieldPath + "[" + index + "]", mediaByField);
            }
            return;
        }
        if (node.isNumber() && isSnapshotMediaField(lastFieldName(fieldPath)) && node.asLong() > 0) {
            mediaByField.put(fieldPath, node.asLong());
        }
    }

    private boolean isSnapshotMediaField(String fieldName) {
        return "mediaid".equals(fieldName) || "mediaids".equals(fieldName)
                || "imageid".equals(fieldName) || "imageids".equals(fieldName)
                || "coverid".equals(fieldName) || "covermediaid".equals(fieldName)
                || "logoid".equals(fieldName) || "logomediaid".equals(fieldName)
                || "thumbnailid".equals(fieldName) || "thumbnailids".equals(fieldName)
                || "iconmediaid".equals(fieldName);
    }

    private String lastFieldName(String fieldPath) {
        int separator = fieldPath.lastIndexOf('.');
        String field = separator >= 0 ? fieldPath.substring(separator + 1) : fieldPath;
        return field.replaceAll("\\[\\d+]$", "").toLowerCase();
    }

    private String toSnapshotBizField(String fieldPath) {
        String prefix = "snapshot:";
        if (prefix.length() + fieldPath.length() <= 64) {
            return prefix + fieldPath;
        }
        return prefix + fieldPath.substring(0, 48) + "_" + Integer.toUnsignedString(fieldPath.hashCode(), 36);
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
        entity.setStatus(MediaAssetStatusEnum.ACTIVE.getCode());
        mediaAssetMapper.updateById(entity);
    }

    private MediaAssetEntity requireAsset(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "媒体ID不能为空");
        }
        MediaAssetEntity entity = mediaAssetMapper.selectOne(new LambdaQueryWrapper<MediaAssetEntity>()
                .eq(MediaAssetEntity::getId, id)
                .eq(MediaAssetEntity::getDeletedMarker, 0L));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "媒体不存在或已被删除");
        }
        return entity;
    }

    private MediaAssetVO toVO(MediaAssetEntity entity) {
        MediaAssetVO vo = new MediaAssetVO();
        vo.setId(entity.getId());
        vo.setMediaType(entity.getMediaType());
        vo.setStatus(MediaAssetStatusEnum.isDeleted(entity.getStatus()) ? "DELETED" : "ACTIVE");
        vo.setOriginalFilename(entity.getOriginalFilename());
        vo.setContentType(entity.getContentType());
        vo.setStoragePath(entity.getStoragePath());
        vo.setPublicUrl(entity.getPublicUrl());
        vo.setAbsoluteUrl(buildAbsoluteUrl(entity.getPublicUrl()));
        vo.setFileSize(entity.getFileSize());
        vo.setUsageTag(StringUtils.hasText(entity.getUsageTag()) ? entity.getUsageTag() : DEFAULT_USAGE_TAG);
        vo.setAltText(entity.getAltText());
        vo.setRemark(entity.getRemark());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
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

    private String normalizeMediaType(String mediaType) {
        String normalized = mediaType.trim().toUpperCase();
        if (!"IMAGE".equals(normalized) && !"DOCUMENT".equals(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "媒体类型不合法");
        }
        return normalized;
    }

    private String normalizeUsageTag(String usageTag) {
        String normalized = usageTag.trim().toUpperCase();
        if (!USAGE_TAGS.contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "媒体用途不合法");
        }
        return normalized;
    }

    private String normalizeStatus(String status) {
        String normalized = status.trim().toUpperCase();
        if (!MediaAssetStatusEnum.ACTIVE.getCode().equals(normalized)
                && !MediaAssetStatusEnum.DELETED.getCode().equals(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "媒体状态不合法");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
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
        snapshot.put("usageTag", entity.getUsageTag());
        snapshot.put("altText", entity.getAltText());
        snapshot.put("remark", entity.getRemark());
        snapshot.put("status", entity.getStatus());
        snapshot.put("version", entity.getVersion());
        snapshot.put("path", entity.getStoragePath());
        snapshot.put("url", entity.getPublicUrl());
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_UPLOAD, TARGET_TYPE, entity.getId(), null, snapshot);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }

    private Map<String, Object> toSnapshot(MediaAssetEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("mediaId", entity.getId());
        snapshot.put("mediaType", entity.getMediaType());
        snapshot.put("originalFilename", entity.getOriginalFilename());
        snapshot.put("contentType", entity.getContentType());
        snapshot.put("fileSize", entity.getFileSize());
        snapshot.put("usageTag", entity.getUsageTag());
        snapshot.put("altText", entity.getAltText());
        snapshot.put("remark", entity.getRemark());
        snapshot.put("status", entity.getStatus());
        snapshot.put("version", entity.getVersion());
        snapshot.put("path", entity.getStoragePath());
        snapshot.put("url", entity.getPublicUrl());
        return snapshot;
    }
}
