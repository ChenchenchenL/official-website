package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.application.portal.PageRenderApplicationService;
import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.utils.DataMaskUtils;
import com.company.officialwebsite.modules.pagebuilder.model.PreviewTokenData;
import com.company.officialwebsite.modules.pagebuilder.service.PreviewTokenService;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageVO;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalPreviewController：Portal 侧受控草稿预览渲染接口。
 * <p>
 * 前端传入预览 Token，后端：
 * <ol>
 *   <li>从 Redis 中解析 Token，获取绑定的 pageId（Token 无效则返回 401）。</li>
 *   <li>加载该页面的当前草稿，复用正式发布链路的数据绑定和可见性过滤。</li>
 *   <li>清除所有 binding 配置元数据后返回 {@link PortalPageVO}。</li>
 * </ol>
 * 安全约束：
 * <ul>
 *   <li>响应头强制设置 {@code Cache-Control: no-store}，禁止任何中间代理缓存预览结果。</li>
 *   <li>预览结果不写入 {@code official:portal:page:*} 正式缓存，完全无状态。</li>
 *   <li>日志中 Token 均通过 {@link DataMaskUtils#maskPreviewToken} 脱敏。</li>
 * </ul>
 */
@RestController
@RequestMapping("/portal/api/page-builder/previews")
public class PortalPreviewController {

    private static final Logger log = LoggerFactory.getLogger(PortalPreviewController.class);

    /**
     * HTTP 响应头：禁止所有缓存层缓存预览结果，保护未发布草稿内容安全。
     */
    private static final String CACHE_CONTROL_NO_STORE = "no-store";

    private final PreviewTokenService previewTokenService;
    private final PageRenderApplicationService pageRenderApplicationService;

    public PortalPreviewController(
            PreviewTokenService previewTokenService,
            PageRenderApplicationService pageRenderApplicationService) {
        this.previewTokenService = previewTokenService;
        this.pageRenderApplicationService = pageRenderApplicationService;
    }

    /**
     * 通过受控预览 Token 渲染草稿页面。
     *
     * @param previewToken Token 路径参数（UUID 格式）
     * @param response     HTTP 响应，用于写入 Cache-Control 头
     * @return 完成数据绑定并清除 binding 元数据的页面渲染模型
     */
    @GetMapping("/{previewToken}")
    public ApiResponse<PortalPageVO> getPreview(
            @PathVariable String previewToken,
            HttpServletResponse response) {

        // 强制禁止任何缓存层缓存预览响应
        response.setHeader("Cache-Control", CACHE_CONTROL_NO_STORE);

        log.info("portal preview request token={}", DataMaskUtils.maskPreviewToken(previewToken));

        // 解析 Token → 获取 pageId（Token 不存在/过期/撤销时抛 PAGE_PREVIEW_TOKEN_EXPIRED → HTTP 401）
        PreviewTokenData tokenData = previewTokenService.resolveToken(previewToken);
        Long pageId = tokenData.getPageId();

        // 使用无缓存预览渲染链路：绑定装配 + 可见性过滤 + binding 元数据清除
        PortalPageVO vo = pageRenderApplicationService.renderDraftForPreview(pageId);

        log.info("portal preview rendered pageId={} token={}", pageId, DataMaskUtils.maskPreviewToken(previewToken));
        return ApiResponse.success(vo);
    }
}
