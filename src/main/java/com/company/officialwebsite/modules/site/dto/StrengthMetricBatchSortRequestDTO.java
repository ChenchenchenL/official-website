package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * StrengthMetricBatchSortRequestDTO：承载后台拖拽排序企业实力核心指标的请求参数。
 * 采用有序 ID 列表模式，后端按顺序自动计算 sort_order，与 HonorBatchSortRequestDTO 保持一致。
 * 传入的 ID 列表必须覆盖全部活跃指标，不允许遗漏或重复。
 */
public class StrengthMetricBatchSortRequestDTO {

    @NotEmpty(message = "排序指标列表不能为空")
    private List<Long> orderedMetricIds;

    public List<Long> getOrderedMetricIds() {
        return orderedMetricIds;
    }

    public void setOrderedMetricIds(List<Long> orderedMetricIds) {
        this.orderedMetricIds = orderedMetricIds;
    }
}
