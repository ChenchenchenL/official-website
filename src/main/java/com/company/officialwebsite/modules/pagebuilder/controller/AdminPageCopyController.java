package com.company.officialwebsite.modules.pagebuilder.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.utils.SecurityUtils;
import com.company.officialwebsite.modules.pagebuilder.dto.PageCopyDTO;
import com.company.officialwebsite.modules.pagebuilder.service.PageCopyService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDefinitionVO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * AdminPageCopyController: 页面复制、模板建页与共享区块影响诊断 Admin 控制器。
 */
@RestController
@RequestMapping("/admin/api/page-builder")
public class AdminPageCopyController {

    private static final Logger log = LoggerFactory.getLogger(AdminPageCopyController.class);

    private final PageCopyService pageCopyService;

    public AdminPageCopyController(PageCopyService pageCopyService) {
        this.pageCopyService = pageCopyService;
    }

    /**
     * 从已有页面或预设模板复制派生创建新页面 (强校验 targetPath 与 targetPageKey 唯一性防冲突)。
     */
    @PostMapping("/pages/copy")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageDefinitionVO> copyPage(@Valid @RequestBody PageCopyDTO dto) {
        String currentOperator = SecurityUtils.getCurrentUsername();
        log.info("admin copy page targetName={} by operator={}", dto.getTargetName(), currentOperator);
        return ApiResponse.success(pageCopyService.copyPage(dto, currentOperator));
    }

    /**
     * 诊断指定共享区块被哪些活跃页面或草稿引用。
     */
    @GetMapping("/shared-blocks/{blockId}/impact")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Map<String, Object>> diagnoseSharedBlockImpact(@PathVariable Long blockId) {
        log.info("admin diagnose shared block impact blockId={}", blockId);
        return ApiResponse.success(pageCopyService.diagnoseSharedBlockImpact(blockId));
    }
}
