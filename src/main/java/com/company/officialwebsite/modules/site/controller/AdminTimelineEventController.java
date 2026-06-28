package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.TimelineEventBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.TimelineEventCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.TimelineEventUpdateRequestDTO;
import com.company.officialwebsite.modules.site.service.TimelineEventService;
import com.company.officialwebsite.modules.site.vo.AdminTimelineEventVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminTimelineEventController：提供后台时间轴节点管理接口。
 */
@Validated
@RestController
@RequestMapping("/admin/api/timeline-events")
public class AdminTimelineEventController {

    private static final Logger log = LoggerFactory.getLogger(AdminTimelineEventController.class);

    private final TimelineEventService timelineEventService;

    public AdminTimelineEventController(TimelineEventService timelineEventService) {
        this.timelineEventService = timelineEventService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<PageResult<AdminTimelineEventVO>> getTimelineEvents(
            @RequestParam(defaultValue = "1") Integer pageNo,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        log.info("get timeline events request pageNo={} pageSize={}", pageNo, pageSize);
        return ApiResponse.success(timelineEventService.getAdminTimelineEventList(pageNo, pageSize));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> createTimelineEvent(@Valid @RequestBody TimelineEventCreateRequestDTO requestDTO) {
        log.info("create timeline event request year={} visible={}", requestDTO.getYear(), requestDTO.getVisible());
        timelineEventService.createTimelineEvent(requestDTO);
        return ApiResponse.success();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateTimelineEvent(
            @PathVariable Long id,
            @Valid @RequestBody TimelineEventUpdateRequestDTO requestDTO) {
        log.info("update timeline event request id={} version={} year={} visible={}",
                id, requestDTO.getVersion(), requestDTO.getYear(), requestDTO.getVisible());
        timelineEventService.updateTimelineEvent(id, requestDTO);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> deleteTimelineEvent(
            @PathVariable Long id,
            @RequestParam("version") @PositiveOrZero(message = "版本号不能为负数") Integer version) {
        log.info("delete timeline event request id={} version={}", id, version);
        timelineEventService.deleteTimelineEvent(id, version);
        return ApiResponse.success();
    }

    @PostMapping("/reorder")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> reorderTimelineEvents(@Valid @RequestBody TimelineEventBatchSortRequestDTO requestDTO) {
        log.info("reorder timeline events request count={}",
                requestDTO.getOrderedTimelineEventIds() == null ? 0 : requestDTO.getOrderedTimelineEventIds().size());
        timelineEventService.reorderTimelineEvents(requestDTO);
        return ApiResponse.success();
    }
}
