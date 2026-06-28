package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.service.TimelineEventService;
import com.company.officialwebsite.modules.site.vo.PortalTimelineEventVO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalTimelineEventController：提供前台公开时间轴节点接口。
 */
@RestController
@RequestMapping("/portal/api/timeline-events")
public class PortalTimelineEventController {

    private static final Logger log = LoggerFactory.getLogger(PortalTimelineEventController.class);

    private final TimelineEventService timelineEventService;

    public PortalTimelineEventController(TimelineEventService timelineEventService) {
        this.timelineEventService = timelineEventService;
    }

    @GetMapping
    public ApiResponse<List<PortalTimelineEventVO>> getTimelineEvents() {
        log.info("get portal timeline events request");
        return ApiResponse.success(timelineEventService.getPortalTimelineEvents());
    }
}
