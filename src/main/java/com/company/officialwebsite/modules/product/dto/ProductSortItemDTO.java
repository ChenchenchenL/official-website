package com.company.officialwebsite.modules.product.dto;

import jakarta.validation.constraints.NotNull;

/**
 * ProductSortItemDTO：产品拖拽排序项的数据传输对象。
 */
public class ProductSortItemDTO {

    @NotNull(message = "产品 ID 不能为空")
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
