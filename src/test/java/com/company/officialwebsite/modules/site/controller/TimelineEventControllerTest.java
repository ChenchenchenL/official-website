package com.company.officialwebsite.modules.site.controller;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.modules.site.dto.TimelineEventCreateRequestDTO;
import com.company.officialwebsite.modules.site.service.TimelineEventService;
import com.company.officialwebsite.support.BaseAdminControllerIntegrationTest;
import com.company.officialwebsite.support.TestConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;

/**
 * TimelineEventControllerTest：验证时间轴节点后台维护与前台暴露的核心契约。
 */
@SpringBootTest
@AutoConfigureMockMvc
class TimelineEventControllerTest extends BaseAdminControllerIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Autowired
    private TimelineEventService timelineEventService;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("DELETE FROM cms_timeline_event WHERE id > 0");
        jdbcTemplate.update("UPDATE cms_timeline_event SET visible = 1, deleted_marker = 0 WHERE id IN (-9301, -9302, -9303, -9304)");
        jdbcTemplate.update("UPDATE cms_timeline_event SET event_year = CASE id WHEN -9301 THEN 2019 WHEN -9302 THEN 2020 WHEN -9303 THEN 2021 WHEN -9304 THEN 2022 END, sort_order = CASE id WHEN -9301 THEN 10 WHEN -9302 THEN 20 WHEN -9303 THEN 30 WHEN -9304 THEN 40 END WHERE id IN (-9301, -9302, -9303, -9304)");
        redisTemplate.delete(portalCacheKeyBuilder.build("timeline_events"));
    }

    @Test
    void adminGetTimelineEvents_shouldReturnUnauthorized_whenNotLoggedIn() throws Exception {
        mockMvc.perform(get("/admin/api/timeline-events"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(TestConstants.AUTH_UNAUTHORIZED));
    }

    @Test
    void portalGetTimelineEvents_shouldReturnDefaultSeedEventsInOrder() throws Exception {
        mockMvc.perform(get("/portal/api/timeline-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS))
                .andExpect(jsonPath("$.data.length()").value(4))
                .andExpect(jsonPath("$.data[0].year").value(2019))
                .andExpect(jsonPath("$.data[1].year").value(2020))
                .andExpect(jsonPath("$.data[2].year").value(2021))
                .andExpect(jsonPath("$.data[3].year").value(2022));
    }

    @Test
    void createTimelineEvent_shouldPersistAndExposeToPortal_whenRequestIsValid() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/timeline-events")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2023,"title":"出海战略启动","description":"正式开拓海外市场。","visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        mockMvc.perform(get("/portal/api/timeline-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title").value(hasItems("出海战略启动")));
    }

    @Test
    void createTimelineEvent_shouldRejectDuplicateTitleInSameYear() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/timeline-events")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2019,"title":"公司成立","description":"重复标题测试。","visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_TIMELINE_TITLE_DUPLICATE));
    }

    @Test
    void createTimelineEvent_shouldAllowSameTitleInDifferentYears() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/timeline-events")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2023,"title":"公司成立","description":"跨年同名测试。","visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
    }

    @Test
    void createTimelineEvent_shouldRejectInvalidYear() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(post("/admin/api/timeline-events")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":1800,"title":"早年事件","description":"年份越界测试。","visible":true}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.SITE_TIMELINE_YEAR_INVALID));
    }

    @Test
    void updateTimelineEvent_shouldChangeYearAndReorder() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long eventId = createTimelineEvent(session, 2023, "待迁移事件", "迁移前描述", true);
        int version = currentVersion(eventId);

        mockMvc.perform(put("/admin/api/timeline-events/{id}", eventId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":%d,"year":2025,"title":"已迁移事件","description":"迁移后描述。","visible":true}
                                """.formatted(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer updatedYear = jdbcTemplate.queryForObject(
                "SELECT event_year FROM cms_timeline_event WHERE id = ?", Integer.class, eventId);
        Assertions.assertEquals(2025, updatedYear);
    }

    @Test
    void updateTimelineEvent_shouldRejectVersionConflict() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long eventId = createTimelineEvent(session, 2023, "并发事件", "并发测试。", true);

        mockMvc.perform(put("/admin/api/timeline-events/{id}", eventId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":999,"year":2023,"title":"并发事件-新版","description":"并发测试。","visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void deleteTimelineEvent_shouldLogicallyDeleteAndAllowRecreation() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long eventId = createTimelineEvent(session, 2023, "待删除事件", "删除测试。", true);
        int version = currentVersion(eventId);

        mockMvc.perform(delete("/admin/api/timeline-events/{id}", eventId)
                        .session(session)
                        .with(csrf())
                        .param("version", String.valueOf(version)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer activeCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM cms_timeline_event WHERE id = ? AND deleted_marker = 0",
                Integer.class, eventId);
        Assertions.assertEquals(0, activeCount == null ? 0 : activeCount);

        mockMvc.perform(post("/admin/api/timeline-events")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":2023,"title":"待删除事件","description":"删除后重建测试。","visible":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
    }

    @Test
    void reorderTimelineEvents_shouldPersistRequestedOrderWithinSameYear() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long id1 = createTimelineEvent(session, 2024, "事件一", "一", true);
        Long id2 = createTimelineEvent(session, 2024, "事件二", "二", true);

        mockMvc.perform(post("/admin/api/timeline-events/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedTimelineEventIds":[%d,%d]}
                                """.formatted(id2, id1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));

        Integer sortOrder1 = jdbcTemplate.queryForObject(
                "SELECT sort_order FROM cms_timeline_event WHERE id = ?", Integer.class, id1);
        Integer sortOrder2 = jdbcTemplate.queryForObject(
                "SELECT sort_order FROM cms_timeline_event WHERE id = ?", Integer.class, id2);
        Assertions.assertTrue(sortOrder2 < sortOrder1, "重排后事件二应排在事件一前面");
    }

    @Test
    void reorderTimelineEvents_shouldRejectCrossYear() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long id2023 = createTimelineEvent(session, 2023, "2023事件", "一", true);
        Long id2024 = createTimelineEvent(session, 2024, "2024事件", "二", true);

        mockMvc.perform(post("/admin/api/timeline-events/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedTimelineEventIds":[%d,%d]}
                                """.formatted(id2024, id2023)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void reorderTimelineEvents_shouldRejectIncompleteSet() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long id1 = createTimelineEvent(session, 2025, "事件A", "A", true);
        createTimelineEvent(session, 2025, "事件B", "B", true);

        mockMvc.perform(post("/admin/api/timeline-events/reorder")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"orderedTimelineEventIds":[%d]}
                                """.formatted(id1)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.STATE_CONFLICT));
    }

    @Test
    void portalGetTimelineEvents_shouldRespectVisibility() throws Exception {
        MockHttpSession session = loginAsAdmin();
        createTimelineEvent(session, 2026, "隐藏事件", "不可见。", false);

        mockMvc.perform(get("/portal/api/timeline-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].title").value(org.hamcrest.Matchers.not(hasItems("隐藏事件"))));
    }

    @Test
    void deleteTimelineEvent_shouldRequireVersionQueryParam() throws Exception {
        MockHttpSession session = loginAsAdmin();
        Long eventId = createTimelineEvent(session, 2023, "待删除事件-缺参", "删除测试。", true);

        mockMvc.perform(delete("/admin/api/timeline-events/{id}", eventId)
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(TestConstants.PARAM_INVALID));
    }

    @Test
    void createTimelineEvent_shouldRejectOverlongTitle_whenServiceCalledDirectly() {
        TimelineEventCreateRequestDTO requestDTO = new TimelineEventCreateRequestDTO();
        requestDTO.setYear(2027);
        requestDTO.setTitle("x".repeat(129));
        requestDTO.setDescription("描述");
        requestDTO.setVisible(true);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> timelineEventService.createTimelineEvent(requestDTO));
        Assertions.assertEquals(TestConstants.PARAM_INVALID, exception.getErrorCode().getCode());
    }

    @Test
    void createTimelineEvent_shouldRejectOverlongDescription_whenServiceCalledDirectly() {
        TimelineEventCreateRequestDTO requestDTO = new TimelineEventCreateRequestDTO();
        requestDTO.setYear(2027);
        requestDTO.setTitle("时间轴事件");
        requestDTO.setDescription("d".repeat(513));
        requestDTO.setVisible(true);

        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> timelineEventService.createTimelineEvent(requestDTO));
        Assertions.assertEquals(TestConstants.PARAM_INVALID, exception.getErrorCode().getCode());
    }

    private Long createTimelineEvent(MockHttpSession session, int year, String title, String description, boolean visible) throws Exception {
        mockMvc.perform(post("/admin/api/timeline-events")
                        .session(session)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"year":%d,"title":"%s","description":"%s","visible":%s}
                                """.formatted(year, title, description, visible)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(TestConstants.SUCCESS));
        return jdbcTemplate.queryForObject(
                "SELECT id FROM cms_timeline_event WHERE event_year = ? AND title = ? AND deleted_marker = 0",
                Long.class, year, title);
    }

    private int currentVersion(Long eventId) {
        Integer version = jdbcTemplate.queryForObject(
                "SELECT version FROM cms_timeline_event WHERE id = ?", Integer.class, eventId);
        return version == null ? 0 : version;
    }
}
