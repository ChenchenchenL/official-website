package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.TimelineEventBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.TimelineEventCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.TimelineEventUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminTimelineEventVO;
import com.company.officialwebsite.modules.site.vo.PortalTimelineEventVO;
import java.util.List;

/**
 * TimelineEventService：封装时间轴节点的后台维护与前台读取能力。
 */
public interface TimelineEventService {

    PageResult<AdminTimelineEventVO> getAdminTimelineEventList(int pageNo, int pageSize);

    void createTimelineEvent(TimelineEventCreateRequestDTO requestDTO);

    void updateTimelineEvent(Long id, TimelineEventUpdateRequestDTO requestDTO);

    void deleteTimelineEvent(Long id, Integer version);

    void reorderTimelineEvents(TimelineEventBatchSortRequestDTO requestDTO);

    List<PortalTimelineEventVO> getPortalTimelineEvents();
}
