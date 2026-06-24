package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.dto.NavigationMenuCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.NavigationMenuOrderRequestDTO;
import com.company.officialwebsite.modules.site.dto.NavigationMenuUpdateRequestDTO;
import com.company.officialwebsite.modules.site.dto.NavigationMenuVisibilityUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.NavigationMenuService;
import com.company.officialwebsite.modules.site.vo.AdminNavigationMenuVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminNavigationMenuController：提供后台顶部导航菜单管理接口。
 */
@RestController
@RequestMapping("/admin/api/site/navigation/menus")
public class AdminNavigationMenuController {

    private final NavigationMenuService navigationMenuService;

    public AdminNavigationMenuController(NavigationMenuService navigationMenuService) {
        this.navigationMenuService = navigationMenuService;
    }

    /**
     * 返回后台当前可编辑的完整导航菜单树。
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminNavigationMenuVO>> getMenuTree() {
        return ApiResponse.success(navigationMenuService.getAdminMenuTree());
    }

    /**
     * 新增一个一级或二级导航菜单项。
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminNavigationMenuVO>> createMenu(
            @Valid @RequestBody NavigationMenuCreateRequestDTO requestDTO) {
        return ApiResponse.success(navigationMenuService.createMenu(requestDTO));
    }

    /**
     * 更新指定菜单项的文案、跳转和显示状态。
     */
    @PutMapping("/{menuId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminNavigationMenuVO>> updateMenu(
            @PathVariable Long menuId,
            @Valid @RequestBody NavigationMenuUpdateRequestDTO requestDTO) {
        return ApiResponse.success(navigationMenuService.updateMenu(menuId, requestDTO));
    }

    /**
     * 单独更新指定菜单项的显示状态。
     */
    @PutMapping("/{menuId}/visibility")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminNavigationMenuVO>> updateVisibility(
            @PathVariable Long menuId,
            @Valid @RequestBody NavigationMenuVisibilityUpdateRequestDTO requestDTO) {
        return ApiResponse.success(navigationMenuService.updateVisibility(menuId, requestDTO));
    }

    /**
     * 删除指定菜单项；一级菜单会级联删除全部活跃二级菜单。
     */
    @DeleteMapping("/{menuId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminNavigationMenuVO>> deleteMenu(@PathVariable Long menuId) {
        return ApiResponse.success(navigationMenuService.deleteMenu(menuId));
    }

    /**
     * 按同级范围提交完整菜单顺序。
     */
    @PutMapping("/order")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<List<AdminNavigationMenuVO>> reorderMenus(
            @Valid @RequestBody NavigationMenuOrderRequestDTO requestDTO) {
        return ApiResponse.success(navigationMenuService.reorderMenus(requestDTO));
    }
}
