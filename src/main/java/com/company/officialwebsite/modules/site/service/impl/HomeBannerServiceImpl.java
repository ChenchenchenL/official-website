package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.enums.MenuTargetTypeEnum;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.dto.HomeBannerButtonDTO;
import com.company.officialwebsite.modules.site.dto.HomeBannerUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.HomeBannerConfigEntity;
import com.company.officialwebsite.modules.site.mapper.HomeBannerConfigMapper;
import com.company.officialwebsite.modules.site.service.HomeBannerService;
import com.company.officialwebsite.modules.site.vo.AdminHomeBannerVO;
import com.company.officialwebsite.modules.site.vo.HomeBannerButtonVO;
import com.company.officialwebsite.modules.site.vo.PortalHomeBannerVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Locale;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * HomeBannerServiceImpl：实现首页首屏主视觉单例的查询、更新、审计和缓存逻辑。
 */
@Service
public class HomeBannerServiceImpl implements HomeBannerService {

    private static final Logger log = LoggerFactory.getLogger(HomeBannerServiceImpl.class);
    private static final String CONFIG_KEY = "default";
    private static final String CACHE_MODULE = "home";
    private static final String CACHE_SEGMENT = "banner";
    private static final String ACTION_UPDATE = "UPDATE_HOME_BANNER";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "HOME_BANNER";
    private static final String FIELD_BACKGROUND_IMAGE = "backgroundImage";
    private static final int MAX_ANCHOR_LENGTH = 64;
    private static final Pattern ANCHOR_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0," + (MAX_ANCHOR_LENGTH - 1) + "}$");

    private final HomeBannerConfigMapper homeBannerConfigMapper;
    private final MediaAssetService mediaAssetService;
    private final AuditLogService auditLogService;
    private final PortalCacheInvalidationSupport portalCacheInvalidationSupport;
    private final PortalCacheKeyBuilder portalCacheKeyBuilder;
    private final RedisTemplate<String, Object> redisTemplate;
    private final OfficialProperties officialProperties;
    private final ObjectMapper objectMapper;

    public HomeBannerServiceImpl(
            HomeBannerConfigMapper homeBannerConfigMapper,
            MediaAssetService mediaAssetService,
            AuditLogService auditLogService,
            PortalCacheInvalidationSupport portalCacheInvalidationSupport,
            PortalCacheKeyBuilder portalCacheKeyBuilder,
            RedisTemplate<String, Object> redisTemplate,
            OfficialProperties officialProperties,
            ObjectMapper objectMapper) {
        this.homeBannerConfigMapper = homeBannerConfigMapper;
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
    public AdminHomeBannerVO getAdminBanner() {
        return toAdminVO(requireConfig());
    }

    @Override
    @Transactional
    public AdminHomeBannerVO updateBanner(HomeBannerUpdateRequestDTO requestDTO) {
        HomeBannerConfigEntity entity = requireConfig();
        log.info("update home banner request configId={} version={} backgroundImageMediaId={}",
                entity.getId(), requestDTO.getVersion(), requestDTO.getBackgroundImageMediaId());
        validateRequest(requestDTO);
        if (!entity.getVersion().equals(requestDTO.getVersion())) {
            log.warn("update home banner rejected by stale version configId={} currentVersion={} requestVersion={}",
                    entity.getId(), entity.getVersion(), requestDTO.getVersion());
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "配置已被其他操作更新，请刷新后重试");
        }

        resolvePublicImageOrNull(requestDTO.getBackgroundImageMediaId(), true);
        AdminHomeBannerVO before = toAdminVO(entity);

        entity.setMainTitle(StringFieldUtils.trimToEmpty(requestDTO.getMainTitle()));
        entity.setSubTitle(StringFieldUtils.trimToEmpty(requestDTO.getSubTitle()));
        entity.setBackgroundImageMediaId(requestDTO.getBackgroundImageMediaId());
        applyButton(entity, requestDTO.getPrimaryButton(), true);
        applyButton(entity, requestDTO.getSecondaryButton(), false);

        int updated = homeBannerConfigMapper.updateById(entity);
        if (updated != 1) {
            log.warn("update home banner failed due to optimistic lock configId={} requestVersion={}",
                    entity.getId(), requestDTO.getVersion());
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "配置已被其他操作更新，请刷新后重试");
        }
        syncVersionAfterUpdate(entity, requestDTO.getVersion());

        mediaAssetService.bindMedia(requestDTO.getBackgroundImageMediaId(), BIZ_MODULE, entity.getId(), FIELD_BACKGROUND_IMAGE);

        MediaAssetEntity updatedBackgroundImage =
                resolvePublicImageOrNull(entity.getBackgroundImageMediaId(), false);
        AdminHomeBannerVO after = toAdminVO(entity, updatedBackgroundImage);
        log.info("update home banner success configId={} previousVersion={} currentVersion={} backgroundImageMediaId={}",
                entity.getId(), before.getVersion(), after.getVersion(), after.getBackgroundImageMediaId());
        auditLogService.recordGenericOperation(BIZ_MODULE, ACTION_UPDATE, TARGET_TYPE, entity.getId(), before, after);
        portalCacheInvalidationSupport.invalidatePortalKey(CACHE_MODULE, CACHE_SEGMENT);
        return after;
    }

    @Override
    @Transactional(readOnly = true)
    public PortalHomeBannerVO getPortalBanner() {
        String cacheKey = portalCacheKeyBuilder.build(CACHE_MODULE, CACHE_SEGMENT);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.convertValue(cached, PortalHomeBannerVO.class);
            }
        } catch (Exception ex) {
            log.error("read portal home banner cache failed key={}", cacheKey, ex);
        }

        HomeBannerConfigEntity entity = requireConfig();
        PortalHomeBannerVO vo = new PortalHomeBannerVO();
        vo.setMainTitle(StringFieldUtils.defaultString(entity.getMainTitle()));
        vo.setSubTitle(StringFieldUtils.defaultString(entity.getSubTitle()));
        vo.setBackgroundImageUrl(resolvePublicImageUrl(entity.getBackgroundImageMediaId()));
        vo.setPrimaryButton(toPortalButton(
                entity.getPrimaryEnabled(),
                entity.getPrimaryText(),
                entity.getPrimaryTargetType(),
                entity.getPrimaryRoutePath(),
                entity.getPrimaryAnchorCode(),
                entity.getPrimaryExternalUrl(),
                entity.getPrimaryOpenInNewTab()));
        vo.setSecondaryButton(toPortalButton(
                entity.getSecondaryEnabled(),
                entity.getSecondaryText(),
                entity.getSecondaryTargetType(),
                entity.getSecondaryRoutePath(),
                entity.getSecondaryAnchorCode(),
                entity.getSecondaryExternalUrl(),
                entity.getSecondaryOpenInNewTab()));
        try {
            Duration ttl = officialProperties.getCache().getDefaultTtl();
            redisTemplate.opsForValue().set(cacheKey, vo, ttl);
        } catch (Exception ex) {
            log.error("write portal home banner cache failed key={}", cacheKey, ex);
        }
        return vo;
    }

    /**
     * 首页首屏配置按单例模型设计，当前只允许存在 config_key=default 的活跃记录。
     */
    private HomeBannerConfigEntity requireConfig() {
        HomeBannerConfigEntity entity = homeBannerConfigMapper.selectOne(new LambdaQueryWrapper<HomeBannerConfigEntity>()
                .eq(HomeBannerConfigEntity::getConfigKey, CONFIG_KEY)
                .eq(HomeBannerConfigEntity::getDeletedMarker, 0L)
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND);
        }
        return entity;
    }

    private void validateRequest(HomeBannerUpdateRequestDTO requestDTO) {
        if (requestDTO.getVersion() == null || requestDTO.getVersion() < 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "版本号不能为负数");
        }
        if (StringFieldUtils.isBlank(requestDTO.getMainTitle())) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "主标题不能为空");
        }
        validateButton(requestDTO.getPrimaryButton(), "主按钮");
        validateButton(requestDTO.getSecondaryButton(), "次按钮");
    }

    private void validateButton(HomeBannerButtonDTO button, String buttonName) {
        if (button == null || button.getEnabled() == null) {
            log.warn("validate home banner button failed button={} reason=missing-enabled", buttonName);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, buttonName + "缺少启用状态");
        }
        if (!Boolean.TRUE.equals(button.getEnabled())) {
            return;
        }
        if (StringFieldUtils.isBlank(button.getText())) {
            log.warn("validate home banner button failed button={} reason=blank-text", buttonName);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, buttonName + "启用时按钮文案不能为空");
        }
        if (StringFieldUtils.isBlank(button.getTargetType())) {
            log.warn("validate home banner button failed button={} reason=blank-targetType", buttonName);
            throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, buttonName + "启用时必须配置跳转类型");
        }
        validateTarget(buttonName, button.getTargetType(), button.getRoutePath(), button.getAnchorCode(),
                button.getExternalUrl(), button.getOpenInNewTab());
    }

    private void applyButton(HomeBannerConfigEntity entity, HomeBannerButtonDTO button, boolean primary) {
        boolean enabled = Boolean.TRUE.equals(button.getEnabled());
        if (primary) {
            entity.setPrimaryEnabled(enabled);
        } else {
            entity.setSecondaryEnabled(enabled);
        }
        if (!enabled) {
            clearButton(entity, primary);
            return;
        }

        MenuTargetTypeEnum targetType = parseTargetType(button.getTargetType());
        String text = StringFieldUtils.trimToEmpty(button.getText());
        String routePath = null;
        String anchorCode = null;
        String externalUrl = null;
        boolean openInNewTab = false;
        switch (targetType) {
            case INTERNAL_ROUTE -> routePath = normalizeRoute(button.getRoutePath());
            case PAGE_ANCHOR -> anchorCode = normalizeAnchor(button.getAnchorCode());
            case EXTERNAL_LINK -> {
                externalUrl = normalizeExternalUrl(button.getExternalUrl());
                openInNewTab = Boolean.TRUE.equals(button.getOpenInNewTab());
            }
            default -> throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, "Banner 按钮不支持分组类型");
        }

        if (primary) {
            entity.setPrimaryText(text);
            entity.setPrimaryTargetType(targetType.name());
            entity.setPrimaryRoutePath(routePath);
            entity.setPrimaryAnchorCode(anchorCode);
            entity.setPrimaryExternalUrl(externalUrl);
            entity.setPrimaryOpenInNewTab(openInNewTab);
        } else {
            entity.setSecondaryText(text);
            entity.setSecondaryTargetType(targetType.name());
            entity.setSecondaryRoutePath(routePath);
            entity.setSecondaryAnchorCode(anchorCode);
            entity.setSecondaryExternalUrl(externalUrl);
            entity.setSecondaryOpenInNewTab(openInNewTab);
        }
    }

    private void clearButton(HomeBannerConfigEntity entity, boolean primary) {
        if (primary) {
            entity.setPrimaryText(null);
            entity.setPrimaryTargetType(null);
            entity.setPrimaryRoutePath(null);
            entity.setPrimaryAnchorCode(null);
            entity.setPrimaryExternalUrl(null);
            entity.setPrimaryOpenInNewTab(false);
            return;
        }
        entity.setSecondaryText(null);
        entity.setSecondaryTargetType(null);
        entity.setSecondaryRoutePath(null);
        entity.setSecondaryAnchorCode(null);
        entity.setSecondaryExternalUrl(null);
        entity.setSecondaryOpenInNewTab(false);
    }

    private void validateTarget(
            String buttonName,
            String targetTypeValue,
            String routePath,
            String anchorCode,
            String externalUrl,
            Boolean openInNewTab) {
        MenuTargetTypeEnum targetType = parseTargetType(targetTypeValue);
        switch (targetType) {
            case GROUP -> throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, buttonName + "不支持分组类型");
            case INTERNAL_ROUTE -> {
                if (StringFieldUtils.trimToNull(routePath) == null
                        || StringFieldUtils.trimToNull(anchorCode) != null
                        || StringFieldUtils.trimToNull(externalUrl) != null) {
                    throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, buttonName + "内部路由必须且只能配置 routePath");
                }
                normalizeRoute(routePath);
                if (Boolean.TRUE.equals(openInNewTab)) {
                    throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, buttonName + "内部路由不允许配置新窗口打开");
                }
            }
            case PAGE_ANCHOR -> {
                if (StringFieldUtils.trimToNull(routePath) != null
                        || StringFieldUtils.trimToNull(anchorCode) == null
                        || StringFieldUtils.trimToNull(externalUrl) != null) {
                    throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, buttonName + "页面锚点必须且只能配置 anchorCode");
                }
                normalizeAnchor(anchorCode);
                if (Boolean.TRUE.equals(openInNewTab)) {
                    throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, buttonName + "页面锚点不允许配置新窗口打开");
                }
            }
            case EXTERNAL_LINK -> {
                if (StringFieldUtils.trimToNull(routePath) != null
                        || StringFieldUtils.trimToNull(anchorCode) != null
                        || StringFieldUtils.trimToNull(externalUrl) == null) {
                    throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, buttonName + "外部链接必须且只能配置 externalUrl");
                }
                normalizeExternalUrl(externalUrl);
            }
            default -> throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID);
        }
    }

    private MenuTargetTypeEnum parseTargetType(String targetType) {
        if (StringFieldUtils.isBlank(targetType)) {
            throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, "Banner 按钮跳转类型不能为空");
        }
        try {
            return MenuTargetTypeEnum.valueOf(targetType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, "Banner 按钮跳转类型不合法");
        }
    }

    private String normalizeRoute(String routePath) {
        String normalizedRoute = StringFieldUtils.trimToNull(routePath);
        if (normalizedRoute == null || !normalizedRoute.startsWith("/") || normalizedRoute.contains("#")
                || normalizedRoute.contains("?") || normalizedRoute.contains("://")) {
            throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, "Banner 内部路由格式不合法");
        }
        return normalizedRoute;
    }

    private String normalizeAnchor(String anchorCode) {
        String normalizedAnchor = StringFieldUtils.trimToNull(anchorCode);
        if (normalizedAnchor != null && normalizedAnchor.startsWith("#")) {
            normalizedAnchor = normalizedAnchor.substring(1);
        }
        if (normalizedAnchor == null || !ANCHOR_PATTERN.matcher(normalizedAnchor).matches()) {
            throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, "Banner 页面锚点格式不合法");
        }
        return normalizedAnchor;
    }

    private String normalizeExternalUrl(String externalUrl) {
        String normalizedUrl = StringFieldUtils.trimToNull(externalUrl);
        if (normalizedUrl == null) {
            throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, "Banner 外部链接不能为空");
        }
        try {
            URI uri = new URI(normalizedUrl);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || host.isBlank()) {
                throw new IllegalArgumentException("scheme or host missing");
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
                throw new IllegalArgumentException("unsupported scheme");
            }
            if (isPrivateOrLoopbackHost(host)) {
                throw new IllegalArgumentException("private host not allowed");
            }
            return normalizedUrl;
        } catch (URISyntaxException | IllegalArgumentException | UnknownHostException ex) {
            throw new BusinessException(ErrorCode.SITE_HOME_BANNER_TARGET_INVALID, "Banner 外部链接格式不合法", ex);
        }
    }

    private AdminHomeBannerVO toAdminVO(HomeBannerConfigEntity entity) {
        MediaAssetEntity backgroundImage = resolvePublicImageOrNull(entity.getBackgroundImageMediaId(), false);
        return toAdminVO(entity, backgroundImage);
    }

    private AdminHomeBannerVO toAdminVO(HomeBannerConfigEntity entity, MediaAssetEntity backgroundImage) {
        AdminHomeBannerVO vo = new AdminHomeBannerVO();
        vo.setId(entity.getId());
        vo.setVersion(entity.getVersion());
        vo.setMainTitle(StringFieldUtils.defaultString(entity.getMainTitle()));
        vo.setSubTitle(StringFieldUtils.defaultString(entity.getSubTitle()));
        vo.setBackgroundImageMediaId(entity.getBackgroundImageMediaId());
        vo.setBackgroundImageUrl(backgroundImage == null ? null : backgroundImage.getPublicUrl());
        vo.setPrimaryButton(toAdminButton(
                entity.getPrimaryEnabled(),
                entity.getPrimaryText(),
                entity.getPrimaryTargetType(),
                entity.getPrimaryRoutePath(),
                entity.getPrimaryAnchorCode(),
                entity.getPrimaryExternalUrl(),
                entity.getPrimaryOpenInNewTab()));
        vo.setSecondaryButton(toAdminButton(
                entity.getSecondaryEnabled(),
                entity.getSecondaryText(),
                entity.getSecondaryTargetType(),
                entity.getSecondaryRoutePath(),
                entity.getSecondaryAnchorCode(),
                entity.getSecondaryExternalUrl(),
                entity.getSecondaryOpenInNewTab()));
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private HomeBannerButtonVO toAdminButton(
            Boolean enabled,
            String text,
            String targetType,
            String routePath,
            String anchorCode,
            String externalUrl,
            Boolean openInNewTab) {
        HomeBannerButtonVO vo = new HomeBannerButtonVO();
        vo.setEnabled(Boolean.TRUE.equals(enabled));
        vo.setText(text);
        vo.setTargetType(targetType);
        vo.setRoutePath(routePath);
        vo.setAnchorCode(anchorCode);
        vo.setExternalUrl(externalUrl);
        vo.setOpenInNewTab(Boolean.TRUE.equals(openInNewTab));
        return vo;
    }

    private HomeBannerButtonVO toPortalButton(
            Boolean enabled,
            String text,
            String targetType,
            String routePath,
            String anchorCode,
            String externalUrl,
            Boolean openInNewTab) {
        if (!Boolean.TRUE.equals(enabled)) {
            return null;
        }
        HomeBannerButtonVO vo = new HomeBannerButtonVO();
        vo.setEnabled(true);
        vo.setText(StringFieldUtils.defaultString(text));
        vo.setTargetType(targetType);
        vo.setRoutePath(routePath);
        vo.setAnchorCode(anchorCode);
        vo.setExternalUrl(externalUrl);
        vo.setOpenInNewTab(Boolean.TRUE.equals(openInNewTab));
        return vo;
    }

    private MediaAssetEntity resolvePublicImageOrNull(Long mediaId, boolean bannerContext) {
        if (mediaId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (BusinessException ex) {
            if (shouldTranslateBannerMediaException(ex)) {
                log.warn("resolve home banner background image failed mediaId={} context={} errorCode={}",
                        mediaId, bannerContext ? "update" : "read", ex.getErrorCode().getCode(), ex);
                throw new BusinessException(ErrorCode.SITE_HOME_BANNER_MEDIA_INVALID, "Banner 背景图资源不可用", ex);
            }
            throw ex;
        }
    }

    private String resolvePublicImageUrl(Long mediaId) {
        if (mediaId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(mediaId).getPublicUrl();
        } catch (BusinessException ex) {
            if (shouldTranslateBannerMediaException(ex)) {
                log.warn("resolve home banner background image url failed mediaId={} errorCode={}",
                        mediaId, ex.getErrorCode().getCode(), ex);
                throw new BusinessException(ErrorCode.SITE_HOME_BANNER_MEDIA_INVALID, "Banner 背景图资源不可用", ex);
            }
            throw ex;
        }
    }

    private boolean shouldTranslateBannerMediaException(BusinessException ex) {
        return ex.getErrorCode() == ErrorCode.COMMON_RESOURCE_NOT_FOUND
                || ex.getErrorCode() == ErrorCode.SITE_HOME_BANNER_MEDIA_INVALID;
    }

    /**
     * MyBatis-Plus 乐观锁通常会把新版本号回写到实体；若未回写，则以请求版本号做一次兼容兜底。
     */
    private void syncVersionAfterUpdate(HomeBannerConfigEntity entity, Integer requestVersion) {
        if (entity.getVersion() == null || entity.getVersion().equals(requestVersion)) {
            entity.setVersion(requestVersion + 1);
        }
    }

    private boolean isPrivateOrLoopbackHost(String host) throws UnknownHostException {
        String normalizedHost = stripIpv6Brackets(host.trim());
        if ("localhost".equalsIgnoreCase(normalizedHost)) {
            return true;
        }
        if (!isIpLiteral(normalizedHost)) {
            return false;
        }
        InetAddress address = InetAddress.getByName(normalizedHost);
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isIpv6UniqueLocal(address);
    }

    private String stripIpv6Brackets(String host) {
        if (host.startsWith("[") && host.endsWith("]") && host.length() > 2) {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }

    private boolean isIpLiteral(String host) {
        if (host.indexOf(':') >= 0) {
            return true;
        }
        return IPV4_LITERAL_PATTERN.matcher(host).matches();
    }

    private boolean isIpv6UniqueLocal(InetAddress address) {
        byte[] raw = address.getAddress();
        return raw.length == 16 && (raw[0] & (byte) 0xfe) == (byte) 0xfc;
    }

    private static final Pattern IPV4_LITERAL_PATTERN = Pattern.compile("^\\d{1,3}(?:\\.\\d{1,3}){3}$");
}
