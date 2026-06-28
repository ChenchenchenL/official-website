package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * TimelineEventBatchSortRequestDTO：承载后台同一年份内批量重排时间轴节点的请求参数。
 */
public class TimelineEventBatchSortRequestDTO {

    @NotEmpty(message = "排序节点列表不能为空")
    private List<Long> orderedTimelineEventIds;

    public List<Long> getOrderedTimelineEventIds() {
        return orderedTimelineEventIds;
    }

    public void setOrderedTimelineEventIds(List<Long> orderedTimelineEventIds) {
        this.orderedTimelineEventIds = orderedTimelineEventIds;
    }
}
