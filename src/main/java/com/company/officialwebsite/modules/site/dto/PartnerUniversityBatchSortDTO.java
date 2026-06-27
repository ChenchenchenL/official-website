package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * PartnerUniversityBatchSortDTO：承载合作高校批量重排请求参数。
 */
public class PartnerUniversityBatchSortDTO {

    @NotEmpty(message = "排序高校列表不能为空")
    private List<Long> orderedIds;

    public List<Long> getOrderedIds() {
        return orderedIds;
    }

    public void setOrderedIds(List<Long> orderedIds) {
        this.orderedIds = orderedIds;
    }
}
