package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.pagebuilder.service.ComponentTemplateService;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateUsageVO;
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
 * AdminComponentTemplateController：后台组件模板/物料管理接口。
 */
@RestController
@RequestMapping("/admin/api/page-builder/component-templates")
public class AdminComponentTemplateController {

    private static final Logger log = LoggerFactory.getLogger(AdminComponentTemplateController.class);

    private final ComponentTemplateService templateService;

    public AdminComponentTemplateController(ComponentTemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * 获取全部已启用的组件模板列表。
     *
     * @return 统一响应的组件模板VO列表
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<ComponentTemplateVO>> getTemplateList() {
        log.info("admin get component template list");
        return ApiResponse.success(templateService.getActiveTemplates());
    }

    /**
     * 根据组件唯一编码获取组件模板详情。
     *
     * @param code 组件唯一编码
     * @return 统一响应的组件模板详情VO
     */
    @GetMapping("/{code}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<ComponentTemplateVO> getTemplateDetail(@PathVariable String code) {
        log.info("admin get component template detail code={}", code);
        return ApiResponse.success(templateService.getTemplateByCode(code));
    }

    /**
     * 查询模板在草稿和线上页面快照中的引用，供代码/迁移托管模板变更前进行影响评估。
     */
    @GetMapping("/{code}/usage")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<ComponentTemplateUsageVO> getTemplateUsage(
            @PathVariable String code,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("admin get component template usage code={} pageNo={} pageSize={}", code, pageNo, pageSize);
        return ApiResponse.success(templateService.getTemplateUsage(code, pageNo, pageSize));
    }
}
