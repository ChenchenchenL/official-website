package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.pagebuilder.converter.ComponentTemplateConverter;
import com.company.officialwebsite.modules.pagebuilder.converter.PageDefinitionConverter;
import com.company.officialwebsite.modules.pagebuilder.entity.ComponentTemplateEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.ComponentTemplateStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.ComponentTemplateMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.ComponentTemplateService;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateUsageVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDefinitionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ComponentTemplateServiceImpl: 组件物料模板管理服务实现类。
 */
@Service
public class ComponentTemplateServiceImpl implements ComponentTemplateService {

    private static final Logger log = LoggerFactory.getLogger(ComponentTemplateServiceImpl.class);

    private final ComponentTemplateMapper templateMapper;
    private final ComponentTemplateConverter templateConverter;
    private final PageDraftMapper pageDraftMapper;
    private final PagePublishSnapshotMapper pagePublishSnapshotMapper;
    private final PageDefinitionMapper pageDefinitionMapper;
    private final PageDefinitionConverter pageDefinitionConverter;

    public ComponentTemplateServiceImpl(
            ComponentTemplateMapper templateMapper,
            ComponentTemplateConverter templateConverter,
            PageDraftMapper pageDraftMapper,
            PagePublishSnapshotMapper pagePublishSnapshotMapper,
            PageDefinitionMapper pageDefinitionMapper,
            PageDefinitionConverter pageDefinitionConverter) {
        this.templateMapper = templateMapper;
        this.templateConverter = templateConverter;
        this.pageDraftMapper = pageDraftMapper;
        this.pagePublishSnapshotMapper = pagePublishSnapshotMapper;
        this.pageDefinitionMapper = pageDefinitionMapper;
        this.pageDefinitionConverter = pageDefinitionConverter;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ComponentTemplateVO> getActiveTemplates() {
        List<ComponentTemplateEntity> entities = templateMapper.selectList(
                new LambdaQueryWrapper<ComponentTemplateEntity>()
                        .eq(ComponentTemplateEntity::getStatus, ComponentTemplateStatusEnum.ACTIVE.name())
                        .orderByAsc(ComponentTemplateEntity::getSortOrder)
                        .orderByAsc(ComponentTemplateEntity::getId)
        );
        log.info("query active component templates count={}", entities.size());
        return templateConverter.toVOList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public ComponentTemplateVO getTemplateByCode(String componentCode) {
        ComponentTemplateEntity entity = templateMapper.selectOne(
                new LambdaQueryWrapper<ComponentTemplateEntity>()
                        .eq(ComponentTemplateEntity::getComponentCode, componentCode)
                        .eq(ComponentTemplateEntity::getStatus, ComponentTemplateStatusEnum.ACTIVE.name())
        );
        if (entity == null) {
            log.warn("component template not found or inactive code={}", componentCode);
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "指定的组件模板不存在或已被禁用");
        }
        log.info("query component template detail success code={}", componentCode);
        return templateConverter.toVO(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public ComponentTemplateUsageVO getTemplateUsage(String componentCode, int pageNo, int pageSize) {
        ComponentTemplateEntity template = templateMapper.selectOne(
                new LambdaQueryWrapper<ComponentTemplateEntity>()
                        .eq(ComponentTemplateEntity::getComponentCode, componentCode)
        );
        if (template == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "指定的组件模板不存在");
        }

        int validPageNo = Math.max(1, pageNo);
        int validPageSize = Math.min(100, Math.max(1, pageSize));
        Set<Long> activeSnapshotPageIds = new LinkedHashSet<>();
        for (PagePublishSnapshotEntity snapshot : pagePublishSnapshotMapper.selectList(
                new LambdaQueryWrapper<PagePublishSnapshotEntity>()
                        .eq(PagePublishSnapshotEntity::getPublishStatus, "ACTIVE"))) {
            if (usesComponent(snapshot.getSnapshotJson(), componentCode)) {
                activeSnapshotPageIds.add(snapshot.getPageId());
            }
        }

        Set<Long> draftPageIds = new LinkedHashSet<>();
        for (PageDraftEntity draft : pageDraftMapper.selectList(new LambdaQueryWrapper<>())) {
            if (usesComponent(draft.getSchemaJson(), componentCode)) {
                draftPageIds.add(draft.getPageId());
            }
        }

        ComponentTemplateUsageVO usage = new ComponentTemplateUsageVO();
        usage.setComponentCode(template.getComponentCode());
        usage.setActiveSnapshotPages(toPageResult(activeSnapshotPageIds, validPageNo, validPageSize));
        usage.setDraftPages(toPageResult(draftPageIds, validPageNo, validPageSize));
        log.info("query component template usage code={} activePages={} draftPages={}",
                componentCode, activeSnapshotPageIds.size(), draftPageIds.size());
        return usage;
    }

    private PageResult<PageDefinitionVO> toPageResult(Set<Long> pageIds, int pageNo, int pageSize) {
        if (pageIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageNo, pageSize);
        }
        Map<Long, PageDefinitionEntity> pagesById = pageDefinitionMapper.selectBatchIds(pageIds).stream()
                .collect(Collectors.toMap(PageDefinitionEntity::getId, Function.identity()));
        List<PageDefinitionVO> allPages = pageIds.stream()
                .map(pagesById::get)
                .filter(java.util.Objects::nonNull)
                .map(pageDefinitionConverter::toVO)
                .toList();
        int fromIndex = Math.min((pageNo - 1) * pageSize, allPages.size());
        int toIndex = Math.min(fromIndex + pageSize, allPages.size());
        return PageResult.of(allPages.subList(fromIndex, toIndex), allPages.size(), pageNo, pageSize);
    }

    private boolean usesComponent(PageSchemaModel schema, String componentCode) {
        if (schema == null || schema.getSections() == null) {
            return false;
        }
        return schema.getSections().stream()
                .map(SectionModel::getComponent)
                .anyMatch(componentCode::equals);
    }
}
