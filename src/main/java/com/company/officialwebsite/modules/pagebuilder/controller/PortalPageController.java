package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftService;
import com.company.officialwebsite.modules.pagebuilder.vo.PagePreviewVO;
import com.company.officialwebsite.application.portal.PageRenderApplicationService;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageMetaVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalPageController：前台页面预览、已发布页面渲染与元数据获取接口。
 */
@RestController
@RequestMapping("/portal/api/page-builder/pages")
public class PortalPageController {

    private static final Logger log = LoggerFactory.getLogger(PortalPageController.class);

    private final PageDraftService pageDraftService;
    private final PageRenderApplicationService pageRenderApplicationService;

    public PortalPageController(PageDraftService pageDraftService, PageRenderApplicationService pageRenderApplicationService) {
        this.pageDraftService = pageDraftService;
        this.pageRenderApplicationService = pageRenderApplicationService;
    }

    /**
     * 通过预览 Token 获取页面草稿的完整 Schema 快照，供前台渲染引擎消费。
     *
     * @param previewToken UUID 格式的预览令牌
     * @return 包含 pageKey、name 及完整 Schema 的预览 VO
     */
    @GetMapping("/preview")
    public ApiResponse<PagePreviewVO> getPagePreview(@RequestParam String previewToken) {
        log.info("portal page preview request token={}", previewToken);
        PagePreviewVO result = pageDraftService.getPreviewData(previewToken);
        return ApiResponse.success(result);
    }

    /**
     * 根据访问路由路径获取已发布的装配页面 Schema 渲染模型。
     *
     * @param routePath 访问路由，例如 /news
     * @return 完整的页面渲染模型
     */
    @GetMapping
    public ApiResponse<PortalPageVO> getPageRender(@RequestParam String routePath) {
        log.info("portal page render request routePath={}", routePath);
        return ApiResponse.success(pageRenderApplicationService.renderPageByRoute(routePath));
    }

    /**
     * 根据页面唯一 Key 获取已发布的页面元数据（SEO与布局）。
     *
     * @param pageKey 页面唯一 Key，例如 news
     * @return 页面元数据信息
     */
    @GetMapping("/{pageKey}/meta")
    public ApiResponse<PortalPageMetaVO> getPageMeta(@PathVariable String pageKey) {
        log.info("portal page meta request pageKey={}", pageKey);
        return ApiResponse.success(pageRenderApplicationService.getPageMeta(pageKey));
    }
}
