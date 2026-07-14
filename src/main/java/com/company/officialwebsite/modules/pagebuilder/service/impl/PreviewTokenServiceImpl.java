package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.DataMaskUtils;
import com.company.officialwebsite.modules.pagebuilder.constants.PageBuilderConstants;
import com.company.officialwebsite.modules.pagebuilder.model.PreviewTokenData;
import com.company.officialwebsite.modules.pagebuilder.service.PreviewTokenService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * PreviewTokenServiceImpl：受控预览 Token 的 Redis 生命周期管理实现。
 * <p>
 * Token 为无规则随机 UUID，不携带任何业务信息。
 * Redis Key 格式：official:admin:page-preview:{token}
 * TTL：1 小时（{@link PageBuilderConstants#PREVIEW_TOKEN_TTL_HOURS}）
 * 日志：所有涉及 Token 的日志均通过 {@link DataMaskUtils#maskPreviewToken} 脱敏，仅打印前 6 位。
 * </p>
 */
@Service
public class PreviewTokenServiceImpl implements PreviewTokenService {

    private static final Logger log = LoggerFactory.getLogger(PreviewTokenServiceImpl.class);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public PreviewTokenServiceImpl(
            RedisTemplate<String, Object> redisTemplate,
            ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public String createToken(Long pageId, String schemaHash, String createdBy) {
        String token = UUID.randomUUID().toString();
        String key = PageBuilderConstants.ADMIN_PAGE_PREVIEW_CACHE_PREFIX + token;

        PreviewTokenData data = new PreviewTokenData(
                pageId,
                schemaHash,
                createdBy,
                LocalDateTime.now()
        );
        redisTemplate.opsForValue().set(key, data, Duration.ofHours(PageBuilderConstants.PREVIEW_TOKEN_TTL_HOURS));

        // 日志只打印掩码，禁止记录完整 token
        log.info("preview token created pageId={} createdBy={} token={} ttl={}h",
                pageId, createdBy, DataMaskUtils.maskPreviewToken(token),
                PageBuilderConstants.PREVIEW_TOKEN_TTL_HOURS);
        return token;
    }

    @Override
    public PreviewTokenData resolveToken(String token) {
        if (token == null || token.isBlank()) {
            throw new BusinessException(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED);
        }

        String key = PageBuilderConstants.ADMIN_PAGE_PREVIEW_CACHE_PREFIX + token;
        Object cached;
        try {
            cached = redisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("preview token redis read failed token={}", DataMaskUtils.maskPreviewToken(token), e);
            throw new BusinessException(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED);
        }

        if (cached == null) {
            log.warn("preview token not found or expired token={}", DataMaskUtils.maskPreviewToken(token));
            throw new BusinessException(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED);
        }

        try {
            return objectMapper.convertValue(cached, PreviewTokenData.class);
        } catch (Exception e) {
            // 坏缓存：主动删除后抛业务异常
            log.warn("preview token data corrupted, deleting token={}", DataMaskUtils.maskPreviewToken(token), e);
            revokeToken(token);
            throw new BusinessException(ErrorCode.PAGE_PREVIEW_TOKEN_EXPIRED);
        }
    }

    @Override
    public void revokeToken(String token) {
        if (token == null || token.isBlank()) {
            return;
        }
        String key = PageBuilderConstants.ADMIN_PAGE_PREVIEW_CACHE_PREFIX + token;
        try {
            Boolean deleted = redisTemplate.delete(key);
            log.info("preview token revoked token={} deleted={}", DataMaskUtils.maskPreviewToken(token), deleted);
        } catch (Exception e) {
            log.warn("preview token revoke failed token={}", DataMaskUtils.maskPreviewToken(token), e);
        }
    }

    @Override
    public LocalDateTime computeExpiresAt() {
        return LocalDateTime.now().plusHours(PageBuilderConstants.PREVIEW_TOKEN_TTL_HOURS);
    }
}
