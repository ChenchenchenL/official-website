package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.PageStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.enums.PublishSnapshotStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.PortalPageRenderService;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageMetaVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalSectionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * PortalPageRenderServiceImpl: 前台页面已发布快照渲染服务实现类（仅加载数据，不装配外部绑定，不涉及缓存）。
 */
@Service
public class PortalPageRenderServiceImpl implements PortalPageRenderService {

    private static final Logger log = LoggerFactory.getLogger(PortalPageRenderServiceImpl.class);

    private final PageDefinitionMapper pageDefinitionMapper;
    private final PagePublishSnapshotMapper pagePublishSnapshotMapper;

    public PortalPageRenderServiceImpl(
            PageDefinitionMapper pageDefinitionMapper,
            PagePublishSnapshotMapper pagePublishSnapshotMapper) {
        this.pageDefinitionMapper = pageDefinitionMapper;
        this.pagePublishSnapshotMapper = pagePublishSnapshotMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public PortalPageVO renderPageByRoute(String routePath) {
        // 校验页面存在且启用
        PageDefinitionEntity page = pageDefinitionMapper.selectOne(
                new LambdaQueryWrapper<PageDefinitionEntity>()
                        .eq(PageDefinitionEntity::getRoutePath, routePath.trim())
                        .eq(PageDefinitionEntity::getStatus, PageStatusEnum.ENABLED.name())
        );
        if (page == null) {
            log.warn("renderPageByRoute failed: routePath={} not found or inactive", routePath);
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND);
        }

        // 获取 ACTIVE 状态的发布快照
        PagePublishSnapshotEntity snapshot = pagePublishSnapshotMapper.selectOne(
                new LambdaQueryWrapper<PagePublishSnapshotEntity>()
                        .eq(PagePublishSnapshotEntity::getPageId, page.getId())
                        .eq(PagePublishSnapshotEntity::getPublishStatus, PublishSnapshotStatusEnum.ACTIVE.name())
        );
        if (snapshot == null) {
            log.warn("renderPageByRoute failed: pageId={} snapshot not found", page.getId());
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "该页面尚未发布");
        }

        PageSchemaModel schema = snapshot.getSnapshotJson();
        if (schema == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "发布快照损坏");
        }

        PortalPageVO vo = new PortalPageVO();
        vo.setPageKey(schema.getPageKey());
        vo.setName(schema.getName());
        vo.setLayout(schema.getLayout());
        vo.setSeo(schema.getSeo());

        List<PortalSectionVO> sectionVos = new ArrayList<>();
        if (schema.getSections() != null) {
            for (SectionModel section : schema.getSections()) {
                // 过滤前台不可见的区块
                if (section.getVisible() != null && !section.getVisible()) {
                    continue;
                }

                PortalSectionVO secVo = new PortalSectionVO();
                secVo.setId(section.getId());
                secVo.setComponent(section.getComponent());
                secVo.setProps(section.getProps());
                secVo.setStyle(section.getStyle());
                secVo.setVisible(section.getVisible());
                secVo.setBinding(section.getBinding()); // 附带 binding 配置，供上层装配

                sectionVos.add(secVo);
            }
        }
        vo.setSections(sectionVos);
        return vo;
    }

    @Override
    @Transactional(readOnly = true)
    public PortalPageMetaVO getPageMeta(String pageKey) {
        // 校验页面存在且启用
        PageDefinitionEntity page = pageDefinitionMapper.selectOne(
                new LambdaQueryWrapper<PageDefinitionEntity>()
                        .eq(PageDefinitionEntity::getPageKey, pageKey.trim())
                        .eq(PageDefinitionEntity::getStatus, PageStatusEnum.ENABLED.name())
        );
        if (page == null) {
            log.warn("getPageMeta failed: pageKey={} not found or inactive", pageKey);
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND);
        }

        // 获取 ACTIVE 状态的发布快照
        PagePublishSnapshotEntity snapshot = pagePublishSnapshotMapper.selectOne(
                new LambdaQueryWrapper<PagePublishSnapshotEntity>()
                        .eq(PagePublishSnapshotEntity::getPageId, page.getId())
                        .eq(PagePublishSnapshotEntity::getPublishStatus, PublishSnapshotStatusEnum.ACTIVE.name())
        );
        if (snapshot == null) {
            log.warn("getPageMeta failed: pageId={} snapshot not found", page.getId());
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "该页面尚未发布");
        }

        PageSchemaModel schema = snapshot.getSnapshotJson();
        if (schema == null) {
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND, "发布快照损坏");
        }

        PortalPageMetaVO vo = new PortalPageMetaVO();
        vo.setPageKey(schema.getPageKey());
        vo.setName(schema.getName());
        vo.setLayout(schema.getLayout());
        vo.setSeo(schema.getSeo());
        return vo;
    }
}
