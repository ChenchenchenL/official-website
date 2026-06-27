package com.company.officialwebsite.modules.product.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * IndustrySolutionBatchSortDTO：行业解决方案批量重排请求参数。
 */
public class IndustrySolutionBatchSortDTO {

    @NotEmpty(message = "排序列表不能为空")
    private List<@NotNull(message = "行业解决方案 ID 不能为空") Long> orderedIds;

    public List<Long> getOrderedIds() {
        return orderedIds;
    }

    public void setOrderedIds(List<Long> orderedIds) {
        this.orderedIds = orderedIds;
    }
}
