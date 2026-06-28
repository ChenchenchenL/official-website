package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import jakarta.validation.constraints.NotEmpty;

/**
 * ValueCardBatchSortRequestDTO：承载后台批量重排核心价值观卡片的请求参数。
 */
public class ValueCardBatchSortRequestDTO {

    @NotEmpty(message = "排序卡片 ID 列表不能为空")
    @NotNull(message = "排序卡片 ID 列表不能为空")
    private List<Long> orderedValueCardIds;

    public List<Long> getOrderedValueCardIds() {
        return orderedValueCardIds;
    }

    public void setOrderedValueCardIds(List<Long> orderedValueCardIds) {
        this.orderedValueCardIds = orderedValueCardIds;
    }
}
