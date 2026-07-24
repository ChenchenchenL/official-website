package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.converter.PageDefinitionConverter;
import com.company.officialwebsite.modules.pagebuilder.dto.PageCopyDTO;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.PublishSnapshotStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.PageCopyService;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDefinitionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PageCopyServiceImpl: 页面复制、模板建页与共享区块影响诊断服务实现类。
 */
@Service
public class PageCopyServiceImpl implements PageCopyService {

    private static final Logger log = LoggerFactory.getLogger(PageCopyServiceImpl.class);

    private final PageDefinitionMapper pageDefinitionMapper;
    private final PageDraftMapper pageDraftMapper;
    private final PagePublishSnapshotMapper pagePublishSnapshotMapper;
    private final PageDraftService pageDraftService;
    private final PageDefinitionConverter pageDefinitionConverter;
    private final ObjectMapper objectMapper;

    public PageCopyServiceImpl(PageDefinitionMapper pageDefinitionMapper,
                               PageDraftMapper pageDraftMapper,
                               PagePublishSnapshotMapper pagePublishSnapshotMapper,
                               PageDraftService pageDraftService,
                               PageDefinitionConverter pageDefinitionConverter,
                               ObjectMapper objectMapper) {
        this.pageDefinitionMapper = pageDefinitionMapper;
        this.pageDraftMapper = pageDraftMapper;
        this.pagePublishSnapshotMapper = pagePublishSnapshotMapper;
        this.pageDraftService = pageDraftService;
        this.pageDefinitionConverter = pageDefinitionConverter;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageDefinitionVO copyPage(PageCopyDTO dto, String operator) {
        log.info("copyPage request: targetName={} targetPath={} targetKey={} sourcePageId={} sourceTemplateCode={}",
                dto.getTargetName(), dto.getTargetPath(), dto.getTargetPageKey(), dto.getSourcePageId(), dto.getSourceTemplateCode());

        if (dto.getSourcePageId() == null && (dto.getSourceTemplateCode() == null || dto.getSourceTemplateCode().trim().isEmpty())) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "必须指定来源页面ID (sourcePageId) 或来源模板编码 (sourceTemplateCode)");
        }

        // 自动归一化 targetPath 带有前斜杠 /
        String normalizedPath = dto.getTargetPath() != null ? dto.getTargetPath().trim() : "";
        if (!normalizedPath.startsWith("/")) {
            normalizedPath = "/" + normalizedPath;
        }
        dto.setTargetPath(normalizedPath);

        // 1. 唯一性防冲突门禁校验 (targetPath 与 targetPageKey 必须强制唯一)
        validateUniquePathAndKey(dto.getTargetPath(), dto.getTargetPageKey());

        // 2. 读取源 Schema 内容
        PageSchemaModel sourceSchema = resolveSourceSchema(dto.getSourcePageId(), dto.getSourceTemplateCode());
        if (sourceSchema == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "无法读取有效的来源页面或模板 Schema");
        }

        // 3. 重新为组件 Section 分配全新的 UUID ID，防止多页面间的组件 ID 混淆冲突
        PageSchemaModel copiedSchema = cloneAndReassignSectionIds(sourceSchema);
        copiedSchema.setName(dto.getTargetName());

        // 4. 创建目标 PageDefinitionEntity
        PageDefinitionEntity targetDef = new PageDefinitionEntity();
        targetDef.setName(dto.getTargetName());
        targetDef.setRoutePath(dto.getTargetPath());
        targetDef.setPageKey(dto.getTargetPageKey());
        targetDef.setPageType("NORMAL");
        targetDef.setStatus("ENABLED");
        targetDef.setVisible(true);
        targetDef.setSortOrder(100);
        targetDef.setSourcePageId(dto.getSourcePageId());
        targetDef.setSourceTemplateCode(dto.getSourceTemplateCode());
        targetDef.setCreatedBy(operator != null ? operator : "system");

        pageDefinitionMapper.insert(targetDef);
        Long targetPageId = targetDef.getId();

        // 5. 初始化目标页面的主草稿 PageDraftEntity
        String remark = dto.getSourcePageId() != null
                ? "从页面 #" + dto.getSourcePageId() + " 复制创建"
                : "从模板 [" + dto.getSourceTemplateCode() + "] 派生创建";

        pageDraftService.saveDraft(targetPageId, copiedSchema, remark, 0, null, operator);

        log.info("copyPage success: targetPageId={} name={}", targetPageId, dto.getTargetName());
        return pageDefinitionConverter.toVO(targetDef);
    }

    @Override
    public Map<String, Object> diagnoseSharedBlockImpact(Long blockId) {
        log.info("diagnoseSharedBlockImpact for blockId={}", blockId);
        if (blockId == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "共享区块ID不能为空");
        }

        // 大表内存优化：仅查询包含 refBlockId 的草稿，并且仅 select 必要的列
        LambdaQueryWrapper<PageDraftEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(PageDraftEntity::getPageId, PageDraftEntity::getSchemaJson)
               .like(PageDraftEntity::getSchemaJson, "refBlockId");
        List<PageDraftEntity> candidateDrafts = pageDraftMapper.selectList(wrapper);
        List<Map<String, Object>> impactedDrafts = new ArrayList<>();

        for (PageDraftEntity draft : candidateDrafts) {
            PageSchemaModel schema = draft.getSchemaJson();
            if (schema != null && schema.getSections() != null) {
                for (SectionModel sec : schema.getSections()) {
                    if (sec.getBinding() != null && sec.getBinding().getQuery() != null) {
                        Object refId = sec.getBinding().getQuery().get("refBlockId");
                        if (refId != null && Objects.equals(String.valueOf(refId), String.valueOf(blockId))) {
                            Map<String, Object> item = new HashMap<>();
                            item.put("pageId", draft.getPageId());
                            item.put("sectionId", sec.getId());
                            item.put("component", sec.getComponent());
                            impactedDrafts.add(item);
                        }
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("blockId", blockId);
        result.put("impactedCount", impactedDrafts.size());
        result.put("impactedList", impactedDrafts);

        return result;
    }

    private void validateUniquePathAndKey(String path, String key) {
        Long pathCount = pageDefinitionMapper.selectCount(
                new LambdaQueryWrapper<PageDefinitionEntity>()
                        .eq(PageDefinitionEntity::getRoutePath, path)
        );
        if (pathCount > 0) {
            log.warn("validateUniquePathAndKey failed: routePath {} is already in use", path);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "路由路径 [" + path + "] 已被其他页面占用");
        }

        Long keyCount = pageDefinitionMapper.selectCount(
                new LambdaQueryWrapper<PageDefinitionEntity>()
                        .eq(PageDefinitionEntity::getPageKey, key)
        );
        if (keyCount > 0) {
            log.warn("validateUniquePathAndKey failed: pageKey {} is already in use", key);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "页面 Key 标识 [" + key + "] 已被其他页面占用");
        }
    }

    private PageSchemaModel resolveSourceSchema(Long sourcePageId, String sourceTemplateCode) {
        if (sourcePageId != null) {
            // 校验来源页面定义必须存在且未被删除
            PageDefinitionEntity pageDef = pageDefinitionMapper.selectById(sourcePageId);
            if (pageDef == null) {
                throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "来源页面 #" + sourcePageId + " 不存在或已被删除");
            }

            // 优先读取来源页面的最新草稿
            PageDraftEntity draft = pageDraftMapper.selectOne(
                    new LambdaQueryWrapper<PageDraftEntity>()
                            .eq(PageDraftEntity::getPageId, sourcePageId)
            );
            if (draft != null && draft.getSchemaJson() != null) {
                return draft.getSchemaJson();
            }

            // 草稿不存在时退而读取已发布 ACTIVE 快照
            PagePublishSnapshotEntity activeSnapshot = pagePublishSnapshotMapper.selectOne(
                    new LambdaQueryWrapper<PagePublishSnapshotEntity>()
                            .eq(PagePublishSnapshotEntity::getPageId, sourcePageId)
                            .eq(PagePublishSnapshotEntity::getPublishStatus, PublishSnapshotStatusEnum.ACTIVE.name())
            );
            if (activeSnapshot != null) {
                return activeSnapshot.getSnapshotJson();
            }

            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "来源页面 #" + sourcePageId + " 没有任何可用 Schema 数据");
        }

        if (sourceTemplateCode != null) {
            // 从预设模板创建默认 Schema
            PageSchemaModel templateSchema = new PageSchemaModel();
            templateSchema.setSchemaVersion(1);
            templateSchema.setName("Template Page");

            SectionModel defaultBanner = new SectionModel();
            defaultBanner.setId("sec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
            defaultBanner.setComponent("HeroBanner");
            defaultBanner.setProps(Map.of("title", "Welcome to Template Page"));

            templateSchema.setSections(List.of(defaultBanner));
            return templateSchema;
        }

        return null;
    }

    private PageSchemaModel cloneAndReassignSectionIds(PageSchemaModel sourceSchema) {
        try {
            String jsonStr = objectMapper.writeValueAsString(sourceSchema);
            PageSchemaModel cloned = objectMapper.readValue(jsonStr, PageSchemaModel.class);
            if (cloned.getSections() != null) {
                for (SectionModel sec : cloned.getSections()) {
                    sec.setId("sec_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8));
                }
            }
            return cloned;
        } catch (Exception e) {
            log.error("cloneAndReassignSectionIds failed: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.COMMON_SYSTEM_ERROR, "克隆派生页面 Schema 失败: " + e.getMessage());
        }
    }
}
