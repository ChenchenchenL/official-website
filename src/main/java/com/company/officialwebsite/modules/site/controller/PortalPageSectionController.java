package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.PageSectionService;
import com.company.officialwebsite.modules.site.vo.PageSectionVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/portal/api/site/page-sections")
public class PortalPageSectionController {

    private final PageSectionService pageSectionService;

    public PortalPageSectionController(PageSectionService pageSectionService) {
        this.pageSectionService = pageSectionService;
    }

    @GetMapping
    public ApiResponse<List<PageSectionVO>> listSections(@RequestParam(defaultValue = "home") String pageCode) {
        return ApiResponse.success(pageSectionService.getPortalSections(pageCode));
    }
}
