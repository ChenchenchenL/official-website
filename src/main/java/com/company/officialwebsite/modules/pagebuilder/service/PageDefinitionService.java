package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionUpdateDTO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDefinitionVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalRouteVO;

import java.util.List;

/**
 * PageDefinitionService: 页面定义生命周期服务管理接口。
 */
public interface PageDefinitionService {

    /**
     * 获取管理后台的全部活跃页面列表（排序：sortOrder ASC, id ASC）。
     */
    List<PageDefinitionVO> getAdminPageList();

    /**
     * 获取页面定义详情。
     */
    PageDefinitionVO getPageDetail(Long id);

    /**
     * 新增页面，并自动在事务内初始化该页面的草稿记录。
     */
    PageDefinitionVO createPage(PageDefinitionCreateDTO dto);

    /**
     * 修改页面元数据，支持乐观锁。
     */
    List<PageDefinitionVO> updatePage(Long id, PageDefinitionUpdateDTO dto);

    /**
     * 逻辑删除页面，并级联逻辑删除页面草稿。
     */
    List<PageDefinitionVO> deletePage(Long id, Integer version);

    /**
     * 显式启用页面定义。
     */
    PageDefinitionVO enablePage(Long id, Integer version);

    /**
     * 显式停用页面定义。
     */
    PageDefinitionVO disablePage(Long id, Integer version);

    /**
     * 查询 Portal 启用的活动页面路由清单（供 Portal 路由注册与 Sitemap 索引）。
     *
     * @param onlyVisible 若为 true，仅返回 visible=true 的站内公开页面；若为 false，返回全部 enabled 页面
     * @return Portal 页面路由清单 VO 列表
     */
    List<PortalRouteVO> listActiveRoutes(Boolean onlyVisible);
}
