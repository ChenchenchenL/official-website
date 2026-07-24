package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.PublishSnapshotStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.PageDiffService;
import com.company.officialwebsite.modules.pagebuilder.service.PageSchemaValidationService;
import com.company.officialwebsite.modules.pagebuilder.vo.PublishReviewVO;
import com.company.officialwebsite.modules.pagebuilder.vo.SchemaDiffItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PageDiffServiceImpl: 页面 Schema 版本差异计算与发布预审服务实现类。
 */
@Service
public class PageDiffServiceImpl implements PageDiffService {

    private static final Logger log = LoggerFactory.getLogger(PageDiffServiceImpl.class);

    private final PageDefinitionMapper pageDefinitionMapper;
    private final PageDraftMapper pageDraftMapper;
    private final PagePublishSnapshotMapper pagePublishSnapshotMapper;
    private final PageSchemaValidationService pageSchemaValidationService;

    public PageDiffServiceImpl(PageDefinitionMapper pageDefinitionMapper,
                               PageDraftMapper pageDraftMapper,
                               PagePublishSnapshotMapper pagePublishSnapshotMapper,
                               PageSchemaValidationService pageSchemaValidationService) {
        this.pageDefinitionMapper = pageDefinitionMapper;
        this.pageDraftMapper = pageDraftMapper;
        this.pagePublishSnapshotMapper = pagePublishSnapshotMapper;
        this.pageSchemaValidationService = pageSchemaValidationService;
    }

    @Override
    public List<SchemaDiffItemVO> comparePageSchema(Long pageId, Long compareVersion) {
        log.info("comparePageSchema for pageId={} compareVersion={}", pageId, compareVersion);
        PageDraftEntity draft = requireActiveDraft(pageId);
        PageSchemaModel draftSchema = draft.getSchemaJson();

        PageSchemaModel targetSchema = null;
        if (compareVersion == null || compareVersion <= 0) {
            // 默认与在线 ACTIVE 快照比对
            PagePublishSnapshotEntity activeSnapshot = queryActiveSnapshot(pageId);
            if (activeSnapshot != null) {
                targetSchema = activeSnapshot.getSnapshotJson();
            }
        } else {
            // 指定版本的快照比对
            PagePublishSnapshotEntity snapshot = pagePublishSnapshotMapper.selectOne(
                    new LambdaQueryWrapper<PagePublishSnapshotEntity>()
                            .eq(PagePublishSnapshotEntity::getPageId, pageId)
                            .eq(PagePublishSnapshotEntity::getVersionNo, compareVersion)
            );
            if (snapshot == null) {
                log.warn("comparePageSchema failed: compareVersion={} snapshot not found for pageId={}", compareVersion, pageId);
                throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "对比的版本快照不存在: " + compareVersion);
            }
            targetSchema = snapshot.getSnapshotJson();
        }

        List<SchemaDiffItemVO> diffs = PageSchemaDiffHelper.compareSchemas(targetSchema, draftSchema);
        log.info("comparePageSchema success for pageId={}, diff count={}", pageId, diffs.size());
        return diffs;
    }

    @Override
    public PublishReviewVO generatePublishReview(Long pageId) {
        log.info("generatePublishReview for pageId={}", pageId);
        PageDefinitionEntity pageDef = pageDefinitionMapper.selectById(pageId);
        if (pageDef == null) {
            log.warn("generatePublishReview failed: pageId={} not found", pageId);
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND);
        }

        PageDraftEntity draft = requireActiveDraft(pageId);
        PageSchemaModel draftSchema = draft.getSchemaJson();
        PagePublishSnapshotEntity activeSnapshot = queryActiveSnapshot(pageId);

        PublishReviewVO reviewVO = new PublishReviewVO();
        reviewVO.setPageId(pageId);
        reviewVO.setPageName(pageDef.getName());
        reviewVO.setDraftVersion(draft.getVersion());
        reviewVO.setDraftSchemaHash(draft.getSchemaHash());

        if (activeSnapshot != null) {
            reviewVO.setActiveVersion(activeSnapshot.getVersionNo());
            reviewVO.setActiveSchemaHash(activeSnapshot.getSnapshotHash());
            if (activeSnapshot.getSnapshotJson() != null && activeSnapshot.getSnapshotJson().getSections() != null) {
                reviewVO.setActiveSectionCount(activeSnapshot.getSnapshotJson().getSections().size());
            }
        }

        if (draftSchema != null && draftSchema.getSections() != null) {
            reviewVO.setDraftSectionCount(draftSchema.getSections().size());
        }

        // 1. 提取受控数据绑定源摘要列表
        List<String> sources = new ArrayList<>();
        if (draftSchema != null && draftSchema.getSections() != null) {
            for (SectionModel sec : draftSchema.getSections()) {
                BindingModel binding = sec.getBinding();
                if (binding != null && binding.getSource() != null && !binding.getSource().trim().isEmpty()) {
                    String src = binding.getSource().trim().toLowerCase();
                    if (!sources.contains(src)) {
                        sources.add(src);
                    }
                }
            }
        }
        reviewVO.setBindingSources(sources);

        // 2. 执行发布前 Schema 预校验
        try {
            pageSchemaValidationService.validateSchema(draftSchema);
            reviewVO.setValidationPassed(true);
            reviewVO.setValidationErrorMessage(null);
        } catch (BusinessException e) {
            log.warn("generatePublishReview: Schema validation warning for pageId={}: {}", pageId, e.getMessage());
            reviewVO.setValidationPassed(false);
            reviewVO.setValidationErrorMessage(e.getMessage());
        } catch (Exception e) {
            log.error("generatePublishReview: Unexpected validation exception for pageId={}", pageId, e);
            reviewVO.setValidationPassed(false);
            reviewVO.setValidationErrorMessage("Schema 校验异常: " + e.getMessage());
        }

        // 3. 计算与 ACTIVE 快照的深层 Diff
        PageSchemaModel activeSchema = activeSnapshot != null ? activeSnapshot.getSnapshotJson() : null;
        reviewVO.setDiffItems(PageSchemaDiffHelper.compareSchemas(activeSchema, draftSchema));

        log.info("generatePublishReview success for pageId={}, validationPassed={}", pageId, reviewVO.isValidationPassed());
        return reviewVO;
    }

    private PageDraftEntity requireActiveDraft(Long pageId) {
        PageDraftEntity draft = pageDraftMapper.selectOne(
                new LambdaQueryWrapper<PageDraftEntity>()
                        .eq(PageDraftEntity::getPageId, pageId)
        );
        if (draft == null) {
            throw new BusinessException(ErrorCode.PAGE_DRAFT_NOT_FOUND);
        }
        return draft;
    }

    private PagePublishSnapshotEntity queryActiveSnapshot(Long pageId) {
        return pagePublishSnapshotMapper.selectOne(
                new LambdaQueryWrapper<PagePublishSnapshotEntity>()
                        .eq(PagePublishSnapshotEntity::getPageId, pageId)
                        .eq(PagePublishSnapshotEntity::getPublishStatus, PublishSnapshotStatusEnum.ACTIVE.name())
        );
    }
}
