package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * HomeMetricCardOrderRequestDTO：承载后台首页核心数据指标卡片重排请求。
 */
public class HomeMetricCardOrderRequestDTO {

    @NotEmpty(message = "排序卡片列表不能为空")
    private List<Long> orderedMetricIds;

    public List<Long> getOrderedMetricIds() {
        return orderedMetricIds;
    }

    public void setOrderedMetricIds(List<Long> orderedMetricIds) {
        this.orderedMetricIds = orderedMetricIds;
    }
}
