package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * HonorBatchSortRequestDTO：承载后台批量重排荣誉标签的请求参数。
 */
public class HonorBatchSortRequestDTO {

    @NotEmpty(message = "排序荣誉列表不能为空")
    private List<Long> orderedHonorIds;

    public List<Long> getOrderedHonorIds() {
        return orderedHonorIds;
    }

    public void setOrderedHonorIds(List<Long> orderedHonorIds) {
        this.orderedHonorIds = orderedHonorIds;
    }
}
