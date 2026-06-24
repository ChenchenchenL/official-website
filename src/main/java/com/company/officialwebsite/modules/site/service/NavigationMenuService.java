package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.modules.site.dto.NavigationMenuCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.NavigationMenuOrderRequestDTO;
import com.company.officialwebsite.modules.site.dto.NavigationMenuUpdateRequestDTO;
import com.company.officialwebsite.modules.site.dto.NavigationMenuVisibilityUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminNavigationMenuVO;
import com.company.officialwebsite.modules.site.vo.PortalNavigationMenuVO;
import java.util.List;

/**
 * NavigationMenuService：封装导航菜单后台维护和前台读取能力。
 */
public interface NavigationMenuService {

    /**
     * 获取后台可编辑的完整导航树。
     */
    List<AdminNavigationMenuVO> getAdminMenuTree();

    /**
     * 新增导航菜单项并返回最新导航树。
     */
    List<AdminNavigationMenuVO> createMenu(NavigationMenuCreateRequestDTO requestDTO);

    /**
     * 更新导航菜单项并返回最新导航树。
     */
    List<AdminNavigationMenuVO> updateMenu(Long menuId, NavigationMenuUpdateRequestDTO requestDTO);

    /**
     * 更新导航菜单显示状态并返回最新导航树。
     */
    List<AdminNavigationMenuVO> updateVisibility(Long menuId, NavigationMenuVisibilityUpdateRequestDTO requestDTO);

    /**
     * 删除导航菜单项并返回最新导航树。
     */
    List<AdminNavigationMenuVO> deleteMenu(Long menuId);

    /**
     * 按同级范围重排导航菜单并返回最新导航树。
     */
    List<AdminNavigationMenuVO> reorderMenus(NavigationMenuOrderRequestDTO requestDTO);

    /**
     * 获取前台公开可见的导航树。
     */
    List<PortalNavigationMenuVO> getPortalMenuTree();
}
