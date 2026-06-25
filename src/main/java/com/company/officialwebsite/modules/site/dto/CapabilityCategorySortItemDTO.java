package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotNull;

/**
 * CapabilityCategorySortItemDTO：承载底座分类批量排序子项的请求参数。
 */
public class CapabilityCategorySortItemDTO {

    @NotNull(message = "分类ID不能为空")
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
