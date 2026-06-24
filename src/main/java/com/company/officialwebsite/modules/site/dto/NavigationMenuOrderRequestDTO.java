package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * NavigationMenuOrderRequestDTO：承载后台同级导航菜单重排请求。
 */
public class NavigationMenuOrderRequestDTO {

    @NotNull(message = "父级菜单不能为空")
    private Long parentId;

    @NotEmpty(message = "排序菜单列表不能为空")
    private List<Long> orderedMenuIds;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<Long> getOrderedMenuIds() {
        return orderedMenuIds;
    }

    public void setOrderedMenuIds(List<Long> orderedMenuIds) {
        this.orderedMenuIds = orderedMenuIds;
    }
}
