package com.company.officialwebsite.modules.casecenter.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * CaseBatchSortDTO：标杆案例批量排序请求参数。
 */
public class CaseBatchSortDTO {

    @NotEmpty(message = "排序列表不能为空")
    private List<@NotNull(message = "案例 ID 不能为空") Long> orderedIds;

    public List<Long> getOrderedIds() {
        return orderedIds;
    }

    public void setOrderedIds(List<Long> orderedIds) {
        this.orderedIds = orderedIds;
    }
}
