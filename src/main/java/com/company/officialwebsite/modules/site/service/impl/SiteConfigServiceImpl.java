package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.dto.SiteConfigUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.SiteConfigEntity;
import com.company.officialwebsite.modules.site.mapper.SiteConfigMapper;
import com.company.officialwebsite.modules.site.service.SiteConfigService;
import com.company.officialwebsite.modules.site.vo.AdminSiteConfigVO;
import com.company.officialwebsite.modules.site.vo.PortalSiteConfigVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SiteConfigServiceImpl：实现站点基础配置单例的查询、更新、审计和缓存逻辑。
 */
@Service
public class SiteConfigServiceImpl implements SiteConfigService {

    private static final Logger log = LoggerFactory.getLogger(SiteConfigServiceImpl.class);
    private static final String CONFIG_KEY = "default";
    private static final String CACHE_SEGMENT = "site-config";

    private final SiteConfigMapper siteConfigMapper;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheInvalidationSupport portalCacheInvalidationSupport;
    private final PortalCacheKeyBuilder portalCacheKeyBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OfficialProperties officialProperties;
    private final ObjectMapper objectMapper;

    public SiteConfigServiceImpl(
            SiteConfigMapper siteConfigMapper,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            PortalCacheInvalidationSupport portalCacheInvalidationSupport,
            PortalCacheKeyBuilder portalCacheKeyBuilder,
            RedisTemplate<String, Object> redisTemplate,
            OfficialProperties officialProperties,
            ObjectMapper objectMapper) {
        this.siteConfigMapper = siteConfigMapper;
        this.mediaAssetService = mediaAssetService;
        this.auditLogService = auditLogService;
        this.portalCacheInvalidationSupport = portalCacheInvalidationSupport;
        this.portalCacheKeyBuilder = portalCacheKeyBuilder;
        this.redisTemplate = redisTemplate;
        this.officialProperties = officialProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminSiteConfigVO getAdminConfig() {
        return toAdminVO(requireConfig());
    }

    @Override
    @Transactional
    public AdminSiteConfigVO updateConfig(SiteConfigUpdateRequestDTO requestDTO) {
        SiteConfigEntity entity = requireConfig();
        validateRequest(requestDTO);
        // 单例配置使用乐观锁控制并发覆盖，避免后台多人编辑时发生静默覆盖。
        if (!entity.getVersion().equals(requestDTO.getVersion())) {
            log.warn("update site config rejected by stale version configId={} currentVersion={} requestVersion={}",
                    entity.getId(), entity.getVersion(), requestDTO.getVersion());
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "配置已被其他操作更新，请刷新后重试");
        }

        MediaAssetEntity lightLogo = resolvePublicImageOrNull(requestDTO.getLogoLightMediaId());
        MediaAssetEntity darkLogo = resolvePublicImageOrNull(requestDTO.getLogoDarkMediaId());
        AdminSiteConfigVO before = toAdminVO(entity);

        entity.setSiteTitle(StringFieldUtils.trimToEmpty(requestDTO.getSiteTitle()));
        entity.setSeoKeywords(StringFieldUtils.trimToEmpty(requestDTO.getSeoKeywords()));
        entity.setSeoDescription(StringFieldUtils.trimToEmpty(requestDTO.getSeoDescription()));
        entity.setBrandSlogan(StringFieldUtils.trimToEmpty(requestDTO.getBrandSlogan()));
        entity.setBrandTagline(StringFieldUtils.trimToEmpty(requestDTO.getBrandTagline()));
        entity.setLogoLightMediaId(requestDTO.getLogoLightMediaId());
        entity.setLogoDarkMediaId(requestDTO.getLogoDarkMediaId());

        int updated = siteConfigMapper.update(
                null,
                new LambdaUpdateWrapper<SiteConfigEntity>()
                        .eq(SiteConfigEntity::getId, entity.getId())
                        .eq(SiteConfigEntity::getVersion, requestDTO.getVersion())
                        .set(SiteConfigEntity::getSiteTitle, entity.getSiteTitle())
                        .set(SiteConfigEntity::getSeoKeywords, entity.getSeoKeywords())
                        .set(SiteConfigEntity::getSeoDescription, entity.getSeoDescription())
                        .set(SiteConfigEntity::getBrandSlogan, entity.getBrandSlogan())
                        .set(SiteConfigEntity::getBrandTagline, entity.getBrandTagline())
                        .set(SiteConfigEntity::getLogoLightMediaId, entity.getLogoLightMediaId())
                        .set(SiteConfigEntity::getLogoDarkMediaId, entity.getLogoDarkMediaId())
                        .setSql("version = version + 1"));
        if (updated != 1) {
            log.warn("update site config failed due to optimistic lock configId={} requestVersion={}",
                    entity.getId(), requestDTO.getVersion());
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "配置已被其他操作更新，请刷新后重试");
        }
        entity.setVersion(entity.getVersion() + 1);

        // Logo 统一通过媒体模块建立绑定关系，避免站点配置直接持有裸文件路径。
        mediaAssetService.bindMedia(requestDTO.getLogoLightMediaId(), "SITE", entity.getId(), "logoLight");
        mediaAssetService.bindMedia(requestDTO.getLogoDarkMediaId(), "SITE", entity.getId(), "logoDark");

        AdminSiteConfigVO after = toAdminVO(entity, lightLogo, darkLogo);
        log.info("update site config success configId={} previousVersion={} currentVersion={} lightLogoMediaId={} darkLogoMediaId={}",
                entity.getId(), before.getVersion(), after.getVersion(), after.getLogoLightMediaId(), after.getLogoDarkMediaId());
        auditLogService.recordSiteConfigUpdate(entity.getId(), before, after);
        // 事务提交后统一失效 Portal 缓存，保证多实例场景下不会继续返回旧站点配置。
        portalCacheInvalidationSupport.invalidatePortalKey(CACHE_SEGMENT);
        return after;
    }

    @Override
    @Transactional(readOnly = true)
    public PortalSiteConfigVO getPortalConfig() {
        String cacheKey = portalCacheKey();
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                // Redis JSON 反序列化在未携带类型信息时可能回落为 Map，这里统一转换保证缓存可命中。
                return objectMapper.convertValue(cached, PortalSiteConfigVO.class);
            }
        } catch (Exception ex) {
            // 缓存异常时降级回源数据库，避免前台因为 Redis 短暂不可用直接失败。
            log.warn("read portal site config cache failed key={}", cacheKey, ex);
        }

        SiteConfigEntity entity = requireConfig();
        PortalSiteConfigVO vo = new PortalSiteConfigVO();
        vo.setSiteTitle(StringFieldUtils.defaultString(entity.getSiteTitle()));
        vo.setSeoKeywords(StringFieldUtils.defaultString(entity.getSeoKeywords()));
        vo.setSeoDescription(StringFieldUtils.defaultString(entity.getSeoDescription()));
        vo.setBrandSlogan(StringFieldUtils.defaultString(entity.getBrandSlogan()));
        vo.setBrandTagline(StringFieldUtils.defaultString(entity.getBrandTagline()));
        vo.setLogoLightUrl(resolvePublicUrl(entity.getLogoLightMediaId()));
        vo.setLogoDarkUrl(resolvePublicUrl(entity.getLogoDarkMediaId()));
        try {
            redisTemplate.opsForValue().set(cacheKey, vo, officialProperties.getCache().getDefaultTtl());
        } catch (Exception ex) {
            // 写缓存失败不影响主流程，由下一次请求继续回源并尝试重建缓存。
            log.warn("write portal site config cache failed key={}", cacheKey, ex);
        }
        return vo;
    }

    /**
     * 站点基础配置按单例模型设计，当前只允许存在 config_key=default 的活跃记录。
     */
    private SiteConfigEntity requireConfig() {
        SiteConfigEntity entity = siteConfigMapper.selectOne(new LambdaQueryWrapper<SiteConfigEntity>()
                .eq(SiteConfigEntity::getConfigKey, CONFIG_KEY)
                .eq(SiteConfigEntity::getDeletedMarker, 0L)
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND);
        }
        return entity;
    }

    private AdminSiteConfigVO toAdminVO(SiteConfigEntity entity) {
        MediaAssetEntity lightLogo = resolvePublicImageOrNull(entity.getLogoLightMediaId());
        MediaAssetEntity darkLogo = resolvePublicImageOrNull(entity.getLogoDarkMediaId());
        return toAdminVO(entity, lightLogo, darkLogo);
    }

    private AdminSiteConfigVO toAdminVO(SiteConfigEntity entity, MediaAssetEntity lightLogo, MediaAssetEntity darkLogo) {
        AdminSiteConfigVO vo = new AdminSiteConfigVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setSiteTitle(StringFieldUtils.defaultString(entity.getSiteTitle()));
        vo.setSeoKeywords(StringFieldUtils.defaultString(entity.getSeoKeywords()));
        vo.setSeoDescription(StringFieldUtils.defaultString(entity.getSeoDescription()));
        vo.setBrandSlogan(StringFieldUtils.defaultString(entity.getBrandSlogan()));
        vo.setBrandTagline(StringFieldUtils.defaultString(entity.getBrandTagline()));
        vo.setLogoLightMediaId(entity.getLogoLightMediaId());
        vo.setLogoDarkMediaId(entity.getLogoDarkMediaId());
        vo.setLogoLightUrl(lightLogo == null ? null : lightLogo.getPublicUrl());
        vo.setLogoDarkUrl(darkLogo == null ? null : darkLogo.getPublicUrl());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private MediaAssetEntity resolvePublicImageOrNull(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        return requireUsableImageOrThrow(mediaId);
    }

    private MediaAssetEntity requireUsableImageOrThrow(Long mediaId) {
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (BusinessException ex) {
            throw new BusinessException(ErrorCode.SITE_LOGO_MEDIA_INVALID, "站点 Logo 资源不可用", ex);
        }
    }

    private String portalCacheKey() {
        return portalCacheKeyBuilder.build(CACHE_SEGMENT);
    }

    private String resolvePublicUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        return requireUsableImageOrThrow(mediaId).getPublicUrl();
    }

    private void validateRequest(SiteConfigUpdateRequestDTO requestDTO) {
        if (requestDTO.getVersion() == null || requestDTO.getVersion() < 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "版本号不能为负数");
        }
        if (StringFieldUtils.isBlank(requestDTO.getSiteTitle())) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "网站标题不能为空");
        }
        if (StringFieldUtils.isBlank(requestDTO.getBrandSlogan())) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "品牌主张不能为空");
        }
    }
}
