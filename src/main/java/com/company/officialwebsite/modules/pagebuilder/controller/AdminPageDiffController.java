package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.pagebuilder.service.PageDiffService;
import com.company.officialwebsite.modules.pagebuilder.vo.PublishReviewVO;
import com.company.officialwebsite.modules.pagebuilder.vo.SchemaDiffItemVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AdminPageDiffController: 后台页面 Schema 版本差异计算与发布预审接口。
 */
@RestController
@RequestMapping("/admin/api/page-builder/pages")
public class AdminPageDiffController {

    private static final Logger log = LoggerFactory.getLogger(AdminPageDiffController.class);

    private final PageDiffService pageDiffService;

    public AdminPageDiffController(PageDiffService pageDiffService) {
        this.pageDiffService = pageDiffService;
    }

    /**
     * 获取指定页面的草稿与在线 ACTIVE 快照 (或指定历史版本) 的深层 Schema 变更明细列表。
     */
    @GetMapping("/{pageId}/diff")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<SchemaDiffItemVO>> getPageDiff(
            @PathVariable Long pageId,
            @RequestParam(required = false) Long compareVersion) {
        log.info("admin get page diff pageId={} compareVersion={}", pageId, compareVersion);
        return ApiResponse.success(pageDiffService.comparePageSchema(pageId, compareVersion));
    }

    /**
     * 获取当前草稿发布前的综合审阅概览视图 (包含发布预校验、绑定源摘要、版本对照与 Diff 列表)。
     */
    @GetMapping("/{pageId}/publish-review")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PublishReviewVO> getPublishReview(@PathVariable Long pageId) {
        log.info("admin get page publish review pageId={}", pageId);
        return ApiResponse.success(pageDiffService.generatePublishReview(pageId));
    }
}
