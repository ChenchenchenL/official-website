package com.company.officialwebsite.modules.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.content.entity.ContentReferenceEntity;
import com.company.officialwebsite.modules.content.entity.ContentRelationEntity;
import com.company.officialwebsite.modules.content.mapper.ContentReferenceMapper;
import com.company.officialwebsite.modules.content.mapper.ContentRelationMapper;
import com.company.officialwebsite.modules.content.service.ContentReferenceGuard;
import java.util.Locale;
import org.springframework.stereotype.Service;

@Service
public class ContentReferenceGuardImpl implements ContentReferenceGuard {

    private static final String MSG_REFERENCED = "该内容正在被引用";

    private final ContentReferenceMapper contentReferenceMapper;
    private final ContentRelationMapper contentRelationMapper;

    public ContentReferenceGuardImpl(
            ContentReferenceMapper contentReferenceMapper,
            ContentRelationMapper contentRelationMapper) {
        this.contentReferenceMapper = contentReferenceMapper;
        this.contentRelationMapper = contentRelationMapper;
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
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_REFERENCED);
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
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, MSG_REFERENCED);
        }
    }
}
