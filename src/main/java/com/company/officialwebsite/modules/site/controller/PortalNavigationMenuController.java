package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.NavigationMenuService;
import com.company.officialwebsite.modules.site.vo.PortalNavigationMenuVO;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalNavigationMenuController：提供前台公开的顶部导航菜单查询接口。
 */
@RestController
@RequestMapping("/portal/api/site/navigation")
public class PortalNavigationMenuController {

    private final NavigationMenuService navigationMenuService;

    public PortalNavigationMenuController(NavigationMenuService navigationMenuService) {
        this.navigationMenuService = navigationMenuService;
    }

    /**
     * 返回前台可直接渲染的导航菜单树，仅包含公开可见项。
     */
    @GetMapping
    public ApiResponse<List<PortalNavigationMenuVO>> getMenuTree() {
        return ApiResponse.success(navigationMenuService.getPortalMenuTree());
    }
}
