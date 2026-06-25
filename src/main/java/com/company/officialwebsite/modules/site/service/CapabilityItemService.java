package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.modules.site.dto.CapabilityItemCreateDTO;
import com.company.officialwebsite.modules.site.dto.CapabilityItemSortItemDTO;
import com.company.officialwebsite.modules.site.dto.CapabilityItemUpdateDTO;
import java.util.List;

/**
 * CapabilityItemService：核心能力具体子项业务接口。
 */
public interface CapabilityItemService {

    /**
     * 创建底座子项
     */
    Long createItem(CapabilityItemCreateDTO dto);

    /**
     * 更新底座子项
     */
    void updateItem(Long id, CapabilityItemUpdateDTO dto);

    /**
     * 逻辑删除底座子项
     */
    void deleteItem(Long id, Integer version);

    /**
     * 批量更新子项排序
     */
    void batchSortItems(List<CapabilityItemSortItemDTO> requestDTO);

    /**
     * 级联删除指定分类下的所有子项（通常在逻辑删除分类时由同一个事务中调用）
     */
    void deleteItemsByCategoryId(Long categoryId);
}
