package com.company.officialwebsite.application.portal;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.site.entity.PromiseContentEntity;
import com.company.officialwebsite.modules.site.entity.PromiseTagEntity;
import com.company.officialwebsite.modules.site.mapper.PromiseContentMapper;
import com.company.officialwebsite.modules.site.mapper.PromiseTagMapper;
import com.company.officialwebsite.modules.site.service.PromiseModuleConstants;
import com.company.officialwebsite.modules.site.vo.PortalPromiseModuleVO;
import com.company.officialwebsite.modules.site.vo.PortalPromiseTagVO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PromisePortalApplicationService：聚合主体宣导文案与承诺标签，对外提供"我们的承诺"模块的 Portal 只读输出。
 */
@Service
public class PromisePortalApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PromisePortalApplicationService.class);

    private final PromiseContentMapper promiseContentMapper;
    private final PromiseTagMapper promiseTagMapper;
    private final PortalCacheSupport portalCacheSupport;

    public PromisePortalApplicationService(
            PromiseContentMapper promiseContentMapper,
            PromiseTagMapper promiseTagMapper,
            PortalCacheSupport portalCacheSupport) {
        this.promiseContentMapper = promiseContentMapper;
        this.promiseTagMapper = promiseTagMapper;
        this.portalCacheSupport = portalCacheSupport;
    }

    @Transactional(readOnly = true)
    public PortalPromiseModuleVO getPortalPromiseModule() {
        String cacheKey = portalCacheSupport.buildKey(PromiseModuleConstants.CACHE_SEGMENT);
        PortalPromiseModuleVO cached = portalCacheSupport.readCache(cacheKey, PortalPromiseModuleVO.class, PromiseModuleConstants.CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        PortalPromiseModuleVO module = assemble();
        portalCacheSupport.writeCache(cacheKey, module, portalCacheSupport.isEmptyResult(module), PromiseModuleConstants.CACHE_SEGMENT);
        return module;
    }

    private PortalPromiseModuleVO assemble() {
        PromiseContentEntity content = promiseContentMapper.selectOne(
                new LambdaQueryWrapper<PromiseContentEntity>()
                        .eq(PromiseContentEntity::getConfigKey, PromiseModuleConstants.CONFIG_KEY)
                        .eq(PromiseContentEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (content == null) {
            log.warn("portal promise content config not found configKey={}", PromiseModuleConstants.CONFIG_KEY);
            throw new BusinessException(ErrorCode.SITE_PROMISE_CONTENT_NOT_FOUND);
        }
        List<PromiseTagEntity> tags = promiseTagMapper.selectList(
                new LambdaQueryWrapper<PromiseTagEntity>()
                        .eq(PromiseTagEntity::getDeletedMarker, 0L)
                        .orderByAsc(PromiseTagEntity::getSortOrder)
                        .orderByAsc(PromiseTagEntity::getId));

        PortalPromiseModuleVO module = new PortalPromiseModuleVO();
        module.setContent(StringFieldUtils.defaultString(content.getContent()));
        module.setTags(tags.stream().map(this::toPortalTagVO).toList());
        return module;
    }

    private PortalPromiseTagVO toPortalTagVO(PromiseTagEntity entity) {
        PortalPromiseTagVO vo = new PortalPromiseTagVO();
        vo.setTagText(StringFieldUtils.defaultString(entity.getTagText()));
        return vo;
    }
}
