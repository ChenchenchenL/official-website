package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotNull;

/**
 * CapabilityItemSortItemDTO：承载底座具体子项批量排序子项的请求参数。
 */
public class CapabilityItemSortItemDTO {

    @NotNull(message = "子项ID不能为空")
    private Long id;

    @NotNull(message = "排序值不能为空")
    private Integer sortOrder;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
