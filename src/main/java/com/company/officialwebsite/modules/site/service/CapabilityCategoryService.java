package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.modules.site.dto.CapabilityCategoryCreateDTO;
import com.company.officialwebsite.modules.site.dto.CapabilityCategorySortItemDTO;
import com.company.officialwebsite.modules.site.dto.CapabilityCategoryUpdateDTO;
import com.company.officialwebsite.modules.site.vo.CapabilityCategoryVO;
import com.company.officialwebsite.modules.site.vo.PortalCapabilityCategoryVO;
import java.util.List;

/**
 * CapabilityCategoryService：核心能力底座分类业务接口。
 */
public interface CapabilityCategoryService {

    /**
     * 获取完整分类与子项树状列表（后台管理端展示）
     */
    List<CapabilityCategoryVO> getAdminCategoryTree();

    /**
     * 创建分类
     */
    Long createCategory(CapabilityCategoryCreateDTO dto);

    /**
     * 更新分类
     */
    void updateCategory(Long id, CapabilityCategoryUpdateDTO dto);

    /**
     * 逻辑删除分类（级联删除子项）
     */
    void deleteCategory(Long id, Integer version);

    /**
     * 批量更新分类排序
     */
    void batchSortCategories(List<CapabilityCategorySortItemDTO> requestDTO);

    /**
     * 获取前台展示的能力底座分类树状列表
     */
    List<PortalCapabilityCategoryVO> getPortalCapabilities();

    /**
     * 校验分类在数据库中是否存在且未被逻辑删除
     */
    boolean categoryExists(Long id);
}
