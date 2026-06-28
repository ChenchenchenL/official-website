package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.site.dto.PromiseContentUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.PromiseContentEntity;
import com.company.officialwebsite.modules.site.mapper.PromiseContentMapper;
import com.company.officialwebsite.modules.site.service.PromiseContentService;
import com.company.officialwebsite.modules.site.service.PromiseModuleConstants;
import com.company.officialwebsite.modules.site.vo.AdminPromiseContentVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PromiseContentServiceImpl：实现主体宣导文案的后台单例维护、审计和缓存失效逻辑。
 */
@Service
public class PromiseContentServiceImpl implements PromiseContentService {

    private static final Logger log = LoggerFactory.getLogger(PromiseContentServiceImpl.class);

    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "PROMISE_CONTENT";
    private static final String ACTION_UPDATE = "UPDATE_PROMISE_CONTENT";
    private static final String MSG_CONTENT_REQUIRED = "主体文案不能为空";

    private final PromiseContentMapper promiseContentMapper;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;

    public PromiseContentServiceImpl(
            PromiseContentMapper promiseContentMapper,
            AuditLogService auditLogService,
            PortalCacheSupport portalCacheSupport) {
        this.promiseContentMapper = promiseContentMapper;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
    }

    @Override
    @Transactional(readOnly = true)
    public AdminPromiseContentVO getAdminPromiseContent() {
        PromiseContentEntity entity = requireConfig();
        return toAdminVO(entity);
    }

    @Override
    @Transactional
    public void updatePromiseContent(PromiseContentUpdateRequestDTO requestDTO) {
        PromiseContentEntity entity = requireConfig();
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        String normalizedContent = normalizeContent(requestDTO.getContent());
        entity.setContent(normalizedContent);
        ConcurrencyHelper.tryUpdate(promiseContentMapper, entity);
        log.info("update promise content success id={} previousVersion={} currentVersion={}",
                entity.getId(), requestDTO.getVersion(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        invalidatePortalCache();
    }

    private PromiseContentEntity requireConfig() {
        PromiseContentEntity entity = promiseContentMapper.selectOne(
                new LambdaQueryWrapper<PromiseContentEntity>()
                        .eq(PromiseContentEntity::getConfigKey, PromiseModuleConstants.CONFIG_KEY)
                        .eq(PromiseContentEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("promise content config not found configKey={}", PromiseModuleConstants.CONFIG_KEY);
            throw new BusinessException(ErrorCode.SITE_PROMISE_CONTENT_NOT_FOUND);
        }
        return entity;
    }

    private String normalizeContent(String content) {
        String normalized = StringFieldUtils.trimToNull(content);
        if (normalized == null) {
            log.warn("promise content blank after trim");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_CONTENT_REQUIRED);
        }
        return normalized;
    }

    private AdminPromiseContentVO toAdminVO(PromiseContentEntity entity) {
        AdminPromiseContentVO vo = new AdminPromiseContentVO();
        vo.setId(entity.getId());
        vo.setContent(StringFieldUtils.defaultString(entity.getContent()));
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private Map<String, Object> toSnapshot(PromiseContentEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("configKey", entity.getConfigKey());
        snapshot.put("content", entity.getContent());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void invalidatePortalCache() {
        portalCacheSupport.invalidatePortalKey(PromiseModuleConstants.CACHE_SEGMENT);
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
