package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * ResearchDirectionBatchSortDTO：承载重点研发方向批量重排请求参数。
 */
public class ResearchDirectionBatchSortDTO {

    @NotEmpty(message = "排序研发方向列表不能为空")
    private List<Long> orderedIds;

    public List<Long> getOrderedIds() {
        return orderedIds;
    }

    public void setOrderedIds(List<Long> orderedIds) {
        this.orderedIds = orderedIds;
    }
}
