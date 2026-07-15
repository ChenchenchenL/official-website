package com.company.officialwebsite.modules.content.service.impl;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.DataMaskUtils;
import com.company.officialwebsite.modules.content.dto.DetailPreviewTokenData;
import com.company.officialwebsite.modules.content.service.DetailPreviewTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DetailPreviewTokenServiceImpl：受控详情预览 Token 的 Redis 生命周期管理实现。
 */
@Service
public class DetailPreviewTokenServiceImpl implements DetailPreviewTokenService {

    private static final Logger log = LoggerFactory.getLogger(DetailPreviewTokenServiceImpl.class);

    private static final String DETAIL_PREVIEW_CACHE_PREFIX = "official:admin:detail-preview:";
    private static final long DETAIL_PREVIEW_TTL_HOURS = 1L;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public DetailPreviewTokenServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String createToken(EditorResourceTypeEnum resourceType, Long resourceId, String draftHash, String createdBy) {
        validateBindings(resourceType, resourceId, draftHash, createdBy);

        String token = UUID.randomUUID().toString();
        String key = DETAIL_PREVIEW_CACHE_PREFIX + token;

        DetailPreviewTokenData data = new DetailPreviewTokenData(
                resourceType,
                resourceId,
                draftHash,
                createdBy,
                LocalDateTime.now()
        );
        redisTemplate.opsForValue().set(key, data, Duration.ofHours(DETAIL_PREVIEW_TTL_HOURS));

        log.info("detail preview token created resourceType={} resourceId={} createdBy={} token={} ttl={}h",
                resourceType, resourceId, createdBy, DataMaskUtils.maskPreviewToken(token), DETAIL_PREVIEW_TTL_HOURS);
        return token;
    }

    @Override
    public DetailPreviewTokenData resolveToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.DETAIL_PREVIEW_TOKEN_EXPIRED);
        }

        String key = DETAIL_PREVIEW_CACHE_PREFIX + token;
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("detail preview token redis read failed token={}", DataMaskUtils.maskPreviewToken(token), e);
            throw new BusinessException(ErrorCode.DETAIL_PREVIEW_TOKEN_EXPIRED);
        }

        if (cached == null) {
            log.warn("detail preview token not found or expired token={}", DataMaskUtils.maskPreviewToken(token));
            throw new BusinessException(ErrorCode.DETAIL_PREVIEW_TOKEN_EXPIRED);
        }

        try {
            return objectMapper.convertValue(cached, DetailPreviewTokenData.class);
        } catch (Exception e) {
            log.warn("detail preview token data corrupted, deleting token={}", DataMaskUtils.maskPreviewToken(token), e);
            revokeToken(token);
            throw new BusinessException(ErrorCode.DETAIL_PREVIEW_TOKEN_EXPIRED);
        }
    }

    @Override
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String key = DETAIL_PREVIEW_CACHE_PREFIX + token;
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("detail preview token revoked token={} deleted={}", DataMaskUtils.maskPreviewToken(token), deleted);
        } catch (Exception e) {
            log.warn("detail preview token revoke failed token={}", DataMaskUtils.maskPreviewToken(token), e);
        }
    }

    @Override
    public LocalDateTime computeExpiresAt() {
        return LocalDateTime.now().plusHours(DETAIL_PREVIEW_TTL_HOURS);
    }

    /**
     * 详情预览 Token 只允许绑定三类详情编辑资源，避免内部调用者创建无法被详情预览链路验证的 Token。
     */
    private void validateBindings(
            EditorResourceTypeEnum resourceType,
            Long resourceId,
            String draftHash,
            String createdBy) {
        if (resourceType != EditorResourceTypeEnum.PRODUCT
                && resourceType != EditorResourceTypeEnum.CASE
                && resourceType != EditorResourceTypeEnum.INDUSTRY_SOLUTION) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "详情预览资源类型不合法");
        }
        if (resourceId == null || resourceId <= 0
                || draftHash == null || draftHash.isBlank()
                || createdBy == null || createdBy.isBlank()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "详情预览 Token 绑定参数不完整");
        }
    }
}
