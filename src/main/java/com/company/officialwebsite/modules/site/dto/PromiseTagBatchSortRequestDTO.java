package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * PromiseTagBatchSortRequestDTO：承载后台批量重排承诺标签的请求参数。
 */
public class PromiseTagBatchSortRequestDTO {

    @NotEmpty(message = "排序标签 ID 列表不能为空")
    private List<Long> orderedPromiseTagIds;

    public List<Long> getOrderedPromiseTagIds() {
        return orderedPromiseTagIds;
    }

    public void setOrderedPromiseTagIds(List<Long> orderedPromiseTagIds) {
        this.orderedPromiseTagIds = orderedPromiseTagIds;
    }
}
