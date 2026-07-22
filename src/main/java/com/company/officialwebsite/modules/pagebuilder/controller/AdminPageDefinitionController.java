package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionUpdateDTO;
import com.company.officialwebsite.modules.pagebuilder.service.PageDefinitionService;
import com.company.officialwebsite.modules.pagebuilder.service.PageDependencyQueryService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDependencyVO;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDefinitionVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AdminPageDefinitionController: 后台页面生命周期及元数据管理接口。
 */
@RestController
@RequestMapping("/admin/api/page-builder/pages")
@Validated
public class AdminPageDefinitionController {

    private static final Logger log = LoggerFactory.getLogger(AdminPageDefinitionController.class);

    private final PageDefinitionService pageDefinitionService;
    private final PageDependencyQueryService pageDependencyQueryService;

    public AdminPageDefinitionController(
            PageDefinitionService pageDefinitionService,
            PageDependencyQueryService pageDependencyQueryService) {
        this.pageDefinitionService = pageDefinitionService;
        this.pageDependencyQueryService = pageDependencyQueryService;
    }

    /**
     * 查询全部活跃的页面定义列表。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<PageDefinitionVO>> getPageList() {
        log.info("Admin request: list all page definitions");
        return ApiResponse.success(pageDefinitionService.getAdminPageList());
    }

    /**
     * 获取指定页面定义的详情。
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageDefinitionVO> getPageDetail(@PathVariable Long id) {
        log.info("Admin request: get page detail id={}", id);
        return ApiResponse.success(pageDefinitionService.getPageDetail(id));
    }

    /**
     * 查询当前已发布页面的媒体、实体和聚合数据源依赖，供上线前排障与引用治理使用。
     */
    @GetMapping("/{id}/dependencies")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<PageDependencyVO>> getPublishedDependencies(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("Admin request: get published page dependencies id={} pageNo={} pageSize={}", id, pageNo, pageSize);
        return ApiResponse.success(pageDependencyQueryService.getPublishedDependencies(id, pageNo, pageSize));
    }

    /**
     * 新增页面定义，级联初始化页面空白草稿。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageDefinitionVO> createPage(@Valid @RequestBody PageDefinitionCreateDTO dto) {
        log.info("Admin request: create page key={}, name={}", dto.getPageKey(), dto.getName());
        return ApiResponse.success(pageDefinitionService.createPage(dto));
    }

    /**
     * 更新页面定义元数据，支持并发乐观锁。
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<PageDefinitionVO>> updatePage(
            @PathVariable Long id,
            @Valid @RequestBody PageDefinitionUpdateDTO dto) {
        log.info("Admin request: update page id={}, version={}", id, dto.getVersion());
        return ApiResponse.success(pageDefinitionService.updatePage(id, dto));
    }

    /**
     * 逻辑删除指定的页面定义，级联逻辑删除该页面的草稿记录。
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<PageDefinitionVO>> deletePage(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        log.info("Admin request: delete page id={}, version={}", id, version);
        return ApiResponse.success(pageDefinitionService.deletePage(id, version));
    }

    /**
     * 显式启用指定的页面定义。
     */
    @PostMapping("/{id}/enable")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageDefinitionVO> enablePage(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        log.info("Admin request: enable page id={}, version={}", id, version);
        return ApiResponse.success(pageDefinitionService.enablePage(id, version));
    }

    /**
     * 显式停用指定的页面定义。
     */
    @PostMapping("/{id}/disable")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageDefinitionVO> disablePage(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        log.info("Admin request: disable page id={}, version={}", id, version);
        return ApiResponse.success(pageDefinitionService.disablePage(id, version));
    }
}
