package com.company.officialwebsite.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.content.entity.ContentReferenceEntity;
import com.company.officialwebsite.modules.content.entity.ContentRelationEntity;
import com.company.officialwebsite.modules.content.mapper.ContentReferenceMapper;
import com.company.officialwebsite.modules.content.mapper.ContentRelationMapper;
import com.company.officialwebsite.modules.content.service.ContentReferenceGuard;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDependencyMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ContentReferenceGuardImpl implements ContentReferenceGuard {

    private static final Logger log = LoggerFactory.getLogger(ContentReferenceGuardImpl.class);

    private static final String MSG_REFERENCED = "该内容正被显式引用或关联，无法下线或删除";
    private static final String MSG_PAGE_REFERENCED = "该内容正被已发布上线（ACTIVE）的页面或页面区块引用，无法下线或删除";

    private final ContentReferenceMapper contentReferenceMapper;
    private final ContentRelationMapper contentRelationMapper;
    private final PageDependencyMapper pageDependencyMapper;

    public ContentReferenceGuardImpl(
            ContentReferenceMapper contentReferenceMapper,
            ContentRelationMapper contentRelationMapper,
            PageDependencyMapper pageDependencyMapper) {
        this.contentReferenceMapper = contentReferenceMapper;
        this.contentRelationMapper = contentRelationMapper;
        this.pageDependencyMapper = pageDependencyMapper;
    }

    @Override
    public void assertNotReferenced(String contentType, Long contentId) {
        if (contentType == null || contentId == null) {
            return;
        }
        String normalizedType = contentType.trim().toUpperCase(Locale.ROOT);
        Long explicitReferences = contentReferenceMapper.selectCount(
                new LambdaQueryWrapper<ContentReferenceEntity>()
                        .eq(ContentReferenceEntity::getDeletedMarker, 0L)
                        .eq(ContentReferenceEntity::getReferencedType, normalizedType)
                        .eq(ContentReferenceEntity::getReferencedId, contentId));
        if (explicitReferences != null && explicitReferences > 0) {
            log.warn("assertNotReferenced failed: content id={} type={} referenced by explicit references count={}", contentId, contentType, explicitReferences);
            throw new BusinessException(ErrorCode.RESOURCE_REFERENCE_CONFLICT, MSG_REFERENCED);
        }

        Long relationReferences = contentRelationMapper.selectCount(
                new LambdaQueryWrapper<ContentRelationEntity>()
                        .eq(ContentRelationEntity::getDeletedMarker, 0L)
                        .and(wrapper -> wrapper
                                .eq(ContentRelationEntity::getSourceType, normalizedType)
                                .eq(ContentRelationEntity::getSourceId, contentId)
                                .or()
                                .eq(ContentRelationEntity::getTargetType, normalizedType)
                                .eq(ContentRelationEntity::getTargetId, contentId)));
        if (relationReferences != null && relationReferences > 0) {
            log.warn("assertNotReferenced failed: content id={} type={} referenced by relation references count={}", contentId, contentType, relationReferences);
            throw new BusinessException(ErrorCode.RESOURCE_REFERENCE_CONFLICT, MSG_REFERENCED);
        }
    }

    @Override
    public void assertNotReferencedByPage(String module, String entityType, Long contentId) {
        if (module == null || entityType == null || contentId == null) {
            return;
        }
        // 1. 显式内容引用与关联检测
        assertNotReferenced(module, contentId);

        // 2. 页面区块级依赖检测
        List<Long> pageIds = pageDependencyMapper.selectActivePageIdsByTarget(module, entityType, String.valueOf(contentId));
        if (pageIds != null && !pageIds.isEmpty()) {
            log.warn("assertNotReferencedByPage failed: module={} entityType={} contentId={} dependent pageIds={}",
                    module, entityType, contentId, pageIds);
            throw new BusinessException(ErrorCode.RESOURCE_REFERENCE_CONFLICT, MSG_PAGE_REFERENCED + " (关联页面 ID 列表: " + pageIds + ")");
        }
    }
}
