package com.company.officialwebsite.modules.lead.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * CooperationDirectionTagBatchSortRequestDTO：承载后台批量重排合作方向标签的请求参数。
 */
public class CooperationDirectionTagBatchSortRequestDTO {

    @NotEmpty(message = "排序标签 ID 列表不能为空")
    private List<Long> orderedCooperationDirectionTagIds;

    public List<Long> getOrderedCooperationDirectionTagIds() {
        return orderedCooperationDirectionTagIds;
    }

    public void setOrderedCooperationDirectionTagIds(List<Long> orderedCooperationDirectionTagIds) {
        this.orderedCooperationDirectionTagIds = orderedCooperationDirectionTagIds;
    }
}
