package com.company.officialwebsite.application.portal.impl;

import com.company.officialwebsite.application.portal.PageBindingResolutionService;
import com.company.officialwebsite.application.portal.PageRenderApplicationService;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.pagebuilder.constants.PageBuilderConstants;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftService;
import com.company.officialwebsite.modules.pagebuilder.service.PortalPageRenderService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageMetaVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalSectionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * PageRenderApplicationServiceImpl: 前台页面渲染装配应用服务实现类。
 */
@Service
public class PageRenderApplicationServiceImpl implements PageRenderApplicationService {

    private static final Logger log = LoggerFactory.getLogger(PageRenderApplicationServiceImpl.class);
    private static final String CACHE_MODULE = "PAGE_BUILDER";

    private final PortalPageRenderService portalPageRenderService;
    private final PageBindingResolutionService bindingResolutionService;
    private final PortalCacheSupport portalCacheSupport;
    private final PageDraftService pageDraftService;

    public PageRenderApplicationServiceImpl(
            PortalPageRenderService portalPageRenderService,
            PageBindingResolutionService bindingResolutionService,
            PortalCacheSupport portalCacheSupport,
            PageDraftService pageDraftService) {
        this.portalPageRenderService = portalPageRenderService;
        this.bindingResolutionService = bindingResolutionService;
        this.portalCacheSupport = portalCacheSupport;
        this.pageDraftService = pageDraftService;
    }

    @Override
    public PortalPageVO renderPageByRoute(String routePath) {
        String cacheKey = PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + routePath.trim();

        // 1. 优先读取已发布的装配后渲染树二级缓存
        PortalPageVO cached = portalCacheSupport.readCache(cacheKey, PortalPageVO.class, CACHE_MODULE);
        if (cached != null) {
            log.info("PageRenderApplication cache hit: routePath={}", routePath);
            return cached;
        }

        // 2. 二级缓存未命中，调用页面构建器服务查询原始快照
        PortalPageVO rawVo = portalPageRenderService.renderPageByRoute(routePath);

        // 3. 遍历区块进行跨模块数据装配
        assembleBindings(rawVo);

        // 4. 将装配好的完整页面 VO 写入缓存
        portalCacheSupport.writeCache(cacheKey, rawVo, false, CACHE_MODULE);
        log.info("PageRenderApplication cache written: routePath={}", routePath);

        return rawVo;
    }

    @Override
    public PortalPageMetaVO getPageMeta(String pageKey) {
        String cacheKey = PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + pageKey.trim();

        // 1. 优先读取元数据缓存
        PortalPageMetaVO cached = portalCacheSupport.readCache(cacheKey, PortalPageMetaVO.class, CACHE_MODULE);
        if (cached != null) {
            log.info("PageRenderApplication Meta cache hit: pageKey={}", pageKey);
            return cached;
        }

        // 2. 调用服务加载快照元数据
        PortalPageMetaVO metaVo = portalPageRenderService.getPageMeta(pageKey);

        // 3. 写入缓存
        portalCacheSupport.writeCache(cacheKey, metaVo, false, CACHE_MODULE);
        log.info("PageRenderApplication Meta cache written: pageKey={}", pageKey);

        return metaVo;
    }

    /**
     * 预览专用链路：从草稿 Schema 装配绑定数据并深度脱敏，完全不读写正式 Portal 缓存。
     * <p>
     * 与 {@link #renderPageByRoute} 的核心区别：
     * <ul>
     *   <li>数据来源为未发布草稿，而非 ACTIVE 快照。</li>
     *   <li>不读取、不写入任何 official:portal:page:* 缓存，预览链路无状态。</li>
     *   <li>草稿 schemaJson 为 null（页面尚未编辑）时直接抛出业务异常。</li>
     * </ul>
     */
    @Override
    public PortalPageVO renderDraftForPreview(Long pageId) {
        PageDraftVO draft = pageDraftService.getDraft(pageId);
        PageSchemaModel schema = draft.getSchemaJson();
        if (schema == null) {
            log.warn("renderDraftForPreview: draft schema is null pageId={}", pageId);
            throw new BusinessException(ErrorCode.PAGE_DRAFT_NOT_FOUND, "草稿内容为空，请先保存内容后再预览");
        }

        // 组装 PortalPageVO（复用与正式发布一致的区块转换逻辑）
        PortalPageVO vo = new PortalPageVO();
        vo.setPageKey(schema.getPageKey());
        vo.setName(schema.getName());
        vo.setLayout(schema.getLayout());
        vo.setSeo(schema.getSeo());

        List<PortalSectionVO> sections = new ArrayList<>();
        if (schema.getSections() != null) {
            for (SectionModel section : schema.getSections()) {
                // 过滤前台不可见区块（与正式渲染行为一致）
                if (Boolean.FALSE.equals(section.getVisible())) {
                    continue;
                }
                PortalSectionVO secVo = new PortalSectionVO();
                secVo.setId(section.getId());
                secVo.setComponent(section.getComponent());
                secVo.setProps(section.getProps());
                secVo.setStyle(section.getStyle());
                secVo.setVisible(section.getVisible());
                secVo.setBinding(section.getBinding()); // 暂存，装配后清除
                sections.add(secVo);
            }
        }
        vo.setSections(sections);

        // 执行绑定装配并清除 binding 节点（与正式发布路径完全一致）
        assembleBindings(vo);

        log.info("renderDraftForPreview completed pageId={} sections={}", pageId, sections.size());
        return vo;
    }

    /**
     * 对页面 VO 中所有区块执行数据绑定解析，并清除 binding 配置元数据。
     * 绑定解析失败时降级为空数据（记录 error 日志），不影响其他区块渲染。
     */
    private void assembleBindings(PortalPageVO vo) {
        if (vo.getSections() == null) {
            return;
        }
        for (PortalSectionVO section : vo.getSections()) {
            if (section.getBinding() != null) {
                try {
                    Object resolvedData = bindingResolutionService.resolveBinding(section.getBinding());
                    section.setBindingData(resolvedData);
                } catch (Exception e) {
                    log.error("Failed to resolve dynamic data binding for sectionId={}, component={}",
                            section.getId(), section.getComponent(), e);
                }
                // 清除绑定配置元数据，禁止暴露内部 binding 结构到前端
                section.setBinding(null);
            }
        }
    }
}

