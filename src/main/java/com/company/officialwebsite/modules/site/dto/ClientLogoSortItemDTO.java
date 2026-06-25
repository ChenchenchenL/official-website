package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * ClientLogoSortItemDTO：承载单个客户 Logo 的排序变更参数。
 */
public class ClientLogoSortItemDTO {

    @NotNull(message = "客户Logo ID 不能为空")
    private Long id;

    @NotNull(message = "排序值不能为空")
    @PositiveOrZero(message = "排序值不能为负数")
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
