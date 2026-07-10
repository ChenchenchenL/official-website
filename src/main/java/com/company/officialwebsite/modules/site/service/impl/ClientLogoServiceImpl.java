package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.content.service.ContentReferenceGuard;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.dto.ClientLogoCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.ClientLogoDeleteRequestDTO;
import com.company.officialwebsite.modules.site.dto.ClientLogoSortItemDTO;
import com.company.officialwebsite.modules.site.dto.ClientLogoUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.ClientLogoEntity;
import com.company.officialwebsite.modules.site.mapper.ClientLogoMapper;
import com.company.officialwebsite.modules.site.service.ClientLogoService;
import com.company.officialwebsite.modules.site.vo.AdminClientLogoVO;
import com.company.officialwebsite.modules.site.vo.PortalClientLogoVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ClientLogoServiceImpl：实现服务客户 Logo 墙的后台维护、审计和前台缓存逻辑。
 */
@Service
public class ClientLogoServiceImpl implements ClientLogoService {

    private static final Logger log = LoggerFactory.getLogger(ClientLogoServiceImpl.class);
    private static final String CACHE_SEGMENT = "client_logos";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "CLIENT_LOGO";
    private static final String ACTION_CREATE = "CREATE_CLIENT_LOGO";
    private static final String ACTION_UPDATE = "UPDATE_CLIENT_LOGO";
    private static final String ACTION_DELETE = "DELETE_CLIENT_LOGO";
    private static final String ACTION_REORDER = "REORDER_CLIENT_LOGO";
    private static final String MEDIA_BIZ_FIELD = "logo";
    private static final int DEFAULT_PAGE_NO = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final ClientLogoMapper clientLogoMapper;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final ContentReferenceGuard contentReferenceGuard;

    public ClientLogoServiceImpl(
            ClientLogoMapper clientLogoMapper,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            PortalCacheSupport portalCacheSupport,
            ContentReferenceGuard contentReferenceGuard) {
        this.clientLogoMapper = clientLogoMapper;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.contentReferenceGuard = contentReferenceGuard;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminClientLogoVO> getAdminClientLogos(int pageNo, int pageSize) {
        int normalizedPageNo = pageNo <= 0 ? DEFAULT_PAGE_NO : pageNo;
        int normalizedPageSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        Page<ClientLogoEntity> page = clientLogoMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                new LambdaQueryWrapper<ClientLogoEntity>()
                        .eq(ClientLogoEntity::getDeletedMarker, 0L)
                        .orderByAsc(ClientLogoEntity::getSortOrder)
                        .orderByAsc(ClientLogoEntity::getId));
        List<AdminClientLogoVO> list = page.getRecords().stream().map(this::toAdminVO).toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional
    public Long createClientLogo(ClientLogoCreateRequestDTO requestDTO) {
        ClientLogoEntity entity = new ClientLogoEntity();
        applyForCreate(entity, requestDTO);
        try {
            clientLogoMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_CLIENT_LOGO_NAME_DUPLICATE);
        }
        mediaAssetService.bindMedia(entity.getLogoId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        invalidatePortalClientLogos();
        log.info("create client logo success clientLogoId={} visible={} sortOrder={}", entity.getId(), entity.getVisible(), entity.getSortOrder());
        return entity.getId();
    }

    @Override
    @Transactional
    public void updateClientLogo(Long clientLogoId, ClientLogoUpdateRequestDTO requestDTO) {
        ClientLogoEntity entity = requireActiveClientLogo(clientLogoId);
        assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        applyForUpdate(entity, requestDTO);
        try {
            tryUpdate(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.SITE_CLIENT_LOGO_NAME_DUPLICATE);
        }
        mediaAssetService.bindMedia(entity.getLogoId(), BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalClientLogos();
        log.info("update client logo success clientLogoId={} version={}", entity.getId(), entity.getVersion());
    }

    @Override
    @Transactional
    public void deleteClientLogo(Long clientLogoId, ClientLogoDeleteRequestDTO requestDTO) {
        ClientLogoEntity entity = requireActiveClientLogo(clientLogoId);
        assertVersion(entity.getVersion(), requestDTO.getVersion());
        contentReferenceGuard.assertNotReferenced(TARGET_TYPE, entity.getId());
        Map<String, Object> before = toSnapshot(entity);
        int deleted = clientLogoMapper.update(
                null,
                new LambdaUpdateWrapper<ClientLogoEntity>()
                        .eq(ClientLogoEntity::getId, entity.getId())
                        .eq(ClientLogoEntity::getDeletedMarker, 0L)
                        .eq(ClientLogoEntity::getVersion, requestDTO.getVersion())
                        .setSql("deleted_marker = id, version = version + 1"));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "客户Logo已被其他操作更新，请刷新后重试");
        }
        mediaAssetService.bindMedia(null, BIZ_MODULE, entity.getId(), MEDIA_BIZ_FIELD);
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalClientLogos();
        log.info("delete client logo success clientLogoId={}", entity.getId());
    }

    @Override
    @Transactional
    public void batchSortClientLogos(List<ClientLogoSortItemDTO> requestDTO) {
        if (requestDTO == null || requestDTO.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能为空");
        }
        List<ClientLogoEntity> activeClientLogos = listActiveClientLogos();
        if (activeClientLogos.isEmpty()) {
            throw new BusinessException(ErrorCode.SITE_CLIENT_LOGO_NOT_FOUND, "排序目标不存在");
        }
        Set<Long> currentIds = new LinkedHashSet<>(activeClientLogos.stream().map(ClientLogoEntity::getId).toList());
        Set<Long> requestIds = new LinkedHashSet<>();
        for (ClientLogoSortItemDTO item : requestDTO) {
            if (!requestIds.add(item.getId())) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复客户Logo");
            }
        }
        if (!requestIds.equals(currentIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖当前客户Logo");
        }
        Map<Long, ClientLogoEntity> entityMap = new HashMap<>();
        for (ClientLogoEntity entity : activeClientLogos) {
            entityMap.put(entity.getId(), entity);
        }
        List<Map<String, Object>> before = activeClientLogos.stream().map(this::toSnapshot).toList();
        for (ClientLogoSortItemDTO item : requestDTO) {
            ClientLogoEntity entity = entityMap.get(item.getId());
            entity.setSortOrder(item.getSortOrder());
            tryUpdate(entity);
        }
        List<Map<String, Object>> after = requestDTO.stream().map(item -> toSnapshot(entityMap.get(item.getId()))).toList();
        recordAudit(ACTION_REORDER, 0L, before, after);
        invalidatePortalClientLogos();
        log.info("reorder client logos success clientLogoCount={}", requestDTO.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalClientLogoVO> getPortalClientLogos() {
        String cacheKey = portalCacheSupport.buildKey(CACHE_SEGMENT);
        List<PortalClientLogoVO> cached = portalCacheSupport.readListCache(cacheKey, PortalClientLogoVO.class, CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        List<PortalClientLogoVO> clientLogos = listVisibleClientLogos().stream().map(this::toPortalVO).toList();
        portalCacheSupport.writeCache(cacheKey, clientLogos, portalCacheSupport.isEmptyResult(clientLogos), CACHE_SEGMENT);
        return clientLogos;
    }

    private void applyForCreate(ClientLogoEntity entity, ClientLogoCreateRequestDTO requestDTO) {
        entity.setName(normalizeName(requestDTO.getName()));
        entity.setIndustry(normalizeIndustry(requestDTO.getIndustry()));
        entity.setLogoId(requireLogo(requestDTO.getLogoId()).getId());
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(resolveSortOrder(requestDTO.getSortOrder()));
        assertNameUnique(entity.getName(), null);
    }

    private void applyForUpdate(ClientLogoEntity entity, ClientLogoUpdateRequestDTO requestDTO) {
        entity.setName(normalizeName(requestDTO.getName()));
        entity.setIndustry(normalizeIndustry(requestDTO.getIndustry()));
        entity.setLogoId(requireLogo(requestDTO.getLogoId()).getId());
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(resolveSortOrder(requestDTO.getSortOrder()));
        assertNameUnique(entity.getName(), entity.getId());
    }

    private ClientLogoEntity requireActiveClientLogo(Long clientLogoId) {
        ClientLogoEntity entity = clientLogoMapper.selectOne(new LambdaQueryWrapper<ClientLogoEntity>()
                .eq(ClientLogoEntity::getId, clientLogoId)
                .eq(ClientLogoEntity::getDeletedMarker, 0L)
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.SITE_CLIENT_LOGO_NOT_FOUND);
        }
        return entity;
    }

    private List<ClientLogoEntity> listActiveClientLogos() {
        return clientLogoMapper.selectList(new LambdaQueryWrapper<ClientLogoEntity>()
                .eq(ClientLogoEntity::getDeletedMarker, 0L)
                .orderByAsc(ClientLogoEntity::getSortOrder)
                .orderByAsc(ClientLogoEntity::getId));
    }

    private List<ClientLogoEntity> listVisibleClientLogos() {
        return clientLogoMapper.selectList(new LambdaQueryWrapper<ClientLogoEntity>()
                .eq(ClientLogoEntity::getDeletedMarker, 0L)
                .eq(ClientLogoEntity::getVisible, true)
                .orderByAsc(ClientLogoEntity::getSortOrder)
                .orderByAsc(ClientLogoEntity::getId));
    }

    private AdminClientLogoVO toAdminVO(ClientLogoEntity entity) {
        MediaAssetEntity logoAsset = requireLogo(entity.getLogoId());
        AdminClientLogoVO vo = new AdminClientLogoVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setIndustry(StringFieldUtils.defaultString(entity.getIndustry()));
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());

        AdminClientLogoVO.MediaFileVO logo = new AdminClientLogoVO.MediaFileVO();
        logo.setId(logoAsset.getId());
        logo.setUrl(logoAsset.getPublicUrl());
        logo.setFileName(logoAsset.getOriginalFilename());
        vo.setLogo(logo);
        return vo;
    }

    private PortalClientLogoVO toPortalVO(ClientLogoEntity entity) {
        MediaAssetEntity logoAsset = requireLogo(entity.getLogoId());
        PortalClientLogoVO vo = new PortalClientLogoVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setIndustry(StringFieldUtils.defaultString(entity.getIndustry()));
        vo.setLogoUrl(logoAsset.getPublicUrl());
        return vo;
    }

    private Map<String, Object> toSnapshot(ClientLogoEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("name", entity.getName());
        snapshot.put("industry", entity.getIndustry());
        snapshot.put("logoId", entity.getLogoId());
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private MediaAssetEntity requireLogo(Long mediaId) {
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.SITE_CLIENT_LOGO_MEDIA_INVALID);
        }
    }

    private void assertNameUnique(String name, Long excludeId) {
        Long count = clientLogoMapper.selectCount(new LambdaQueryWrapper<ClientLogoEntity>()
                .eq(ClientLogoEntity::getName, name)
                .eq(ClientLogoEntity::getDeletedMarker, 0L)
                .ne(excludeId != null, ClientLogoEntity::getId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(ErrorCode.SITE_CLIENT_LOGO_NAME_DUPLICATE);
        }
    }

    private void tryUpdate(ClientLogoEntity entity) {
        Integer requestVersion = entity.getVersion();
        int updated = clientLogoMapper.updateById(entity);
        if (updated != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "客户Logo已被其他操作更新，请刷新后重试");
        }
        if (entity.getVersion() == null || entity.getVersion().equals(requestVersion)) {
            entity.setVersion(requestVersion + 1);
        }
    }

    private void assertVersion(Integer currentVersion, Integer requestVersion) {
        if (requestVersion == null || requestVersion < 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "版本号不能为负数");
        }
        if (!Objects.equals(currentVersion, requestVersion)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "客户Logo已被其他操作更新，请刷新后重试");
        }
    }

    private String normalizeName(String name) {
        String normalized = StringFieldUtils.trimToNull(name);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "客户名称不能为空");
        }
        return normalized;
    }

    private String normalizeIndustry(String industry) {
        return StringFieldUtils.trimToNull(industry);
    }

    private Integer resolveSortOrder(Integer sortOrder) {
        return sortOrder == null ? 99 : sortOrder;
    }

    private void invalidatePortalClientLogos() {
        portalCacheSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
