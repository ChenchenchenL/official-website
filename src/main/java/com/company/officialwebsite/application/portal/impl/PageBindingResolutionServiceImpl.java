package com.company.officialwebsite.application.portal.impl;

import com.company.officialwebsite.application.portal.PageBindingResolutionService;
import com.company.officialwebsite.application.portal.PromisePortalApplicationService;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.casecenter.service.CaseService;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO;
import com.company.officialwebsite.modules.lead.service.ContactInfoService;
import com.company.officialwebsite.modules.lead.service.CooperationDirectionTagService;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.product.service.IndustrySolutionService;
import com.company.officialwebsite.modules.product.service.ProductService;
import com.company.officialwebsite.modules.product.vo.PortalProductVO;
import com.company.officialwebsite.modules.site.service.HomeMetricCardService;
import com.company.officialwebsite.modules.site.service.NavigationMenuService;
import com.company.officialwebsite.modules.site.service.SiteConfigService;
import com.company.officialwebsite.modules.site.service.TimelineEventService;
import com.company.officialwebsite.modules.site.service.ValueCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * PageBindingResolutionServiceImpl: 页面数据绑定解析服务应用层实现类。
 */
@Service("portalPageBindingResolutionService")
public class PageBindingResolutionServiceImpl implements PageBindingResolutionService {

    private static final Logger log = LoggerFactory.getLogger(PageBindingResolutionServiceImpl.class);

    private final SiteConfigService siteConfigService;
    private final NavigationMenuService navigationMenuService;
    private final HomeMetricCardService homeMetricCardService;
    private final ProductService productService;
    private final IndustrySolutionService industrySolutionService;
    private final CaseService caseService;
    private final TimelineEventService timelineEventService;
    private final ValueCardService valueCardService;
    private final PromisePortalApplicationService promisePortalApplicationService;
    private final ContactInfoService contactInfoService;
    private final CooperationDirectionTagService cooperationDirectionTagService;

    public PageBindingResolutionServiceImpl(
            SiteConfigService siteConfigService,
            NavigationMenuService navigationMenuService,
            HomeMetricCardService homeMetricCardService,
            ProductService productService,
            IndustrySolutionService industrySolutionService,
            CaseService caseService,
            TimelineEventService timelineEventService,
            ValueCardService valueCardService,
            PromisePortalApplicationService promisePortalApplicationService,
            ContactInfoService contactInfoService,
            CooperationDirectionTagService cooperationDirectionTagService) {
        this.siteConfigService = siteConfigService;
        this.navigationMenuService = navigationMenuService;
        this.homeMetricCardService = homeMetricCardService;
        this.productService = productService;
        this.industrySolutionService = industrySolutionService;
        this.caseService = caseService;
        this.timelineEventService = timelineEventService;
        this.valueCardService = valueCardService;
        this.promisePortalApplicationService = promisePortalApplicationService;
        this.contactInfoService = contactInfoService;
        this.cooperationDirectionTagService = cooperationDirectionTagService;
    }

    @Override
    public Object resolveBinding(BindingModel binding) {
        if (binding == null || binding.getMode() == null) {
            return null;
        }

        String mode = binding.getMode().trim().toUpperCase();
        if ("STATIC".equals(mode)) {
            return null;
        }

        String source = binding.getSource();
        if (source == null || source.trim().isEmpty()) {
            return null;
        }

        source = source.trim().toLowerCase();
        log.info("Application layer resolving data binding: mode={}, source={}", mode, source);

        switch (source) {
            case "site_config":
                return siteConfigService.getPortalConfig();
            case "navigation_menu":
                return navigationMenuService.getPortalMenuTree();
            case "home_metric_card":
                return homeMetricCardService.getPortalMetricCards();
            case "product":
                return resolveProduct(binding.getQuery());
            case "industry_solution":
                return industrySolutionService.getPortalIndustrySolutions();
            case "case":
                return resolveCase(binding.getQuery());
            case "timeline_event":
                return timelineEventService.getPortalTimelineEvents();
            case "value_card":
                return valueCardService.getPortalValueCards();
            case "promise_tag":
                return promisePortalApplicationService.getPortalPromiseModule();
            case "contact_info":
                return contactInfoService.getPortalContactInfo();
            case "cooperation_direction_tag":
                return cooperationDirectionTagService.getPortalCooperationDirectionTagList();
            default:
                log.warn("Unsupported binding source: {}", source);
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "不支持的绑定源: " + source);
        }
    }

    private Object resolveProduct(Map<String, Object> query) {
        List<PortalProductVO> list = productService.getPortalProducts();
        if (query == null || query.isEmpty()) {
            return list;
        }

        // 1. 单产品 ID 过滤
        if (query.containsKey("id") && query.get("id") != null) {
            try {
                long targetId = Long.parseLong(query.get("id").toString().trim());
                return list.stream()
                        .filter(p -> Objects.equals(p.getId(), targetId))
                        .findFirst()
                        .orElse(null);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // 2. 批量产品 ID 列表过滤
        if (query.containsKey("ids")) {
            Object idsObj = query.get("ids");
            List<Long> targetIds = parseLongList(idsObj);
            if (!targetIds.isEmpty()) {
                list = list.stream()
                        .filter(p -> targetIds.contains(p.getId()))
                        .collect(Collectors.toList());
            }
        }

        // 3. limit / pageSize 数量截取
        int limit = getLimitFromQuery(query);
        if (limit > 0 && list.size() > limit) {
            return list.stream().limit(limit).collect(Collectors.toList());
        }

        return list;
    }

    private Object resolveCase(Map<String, Object> query) {
        List<PortalCaseVO> list = caseService.getPortalCases();
        if (query == null || query.isEmpty()) {
            return list;
        }

        // 1. 单案例 ID 过滤
        if (query.containsKey("id") && query.get("id") != null) {
            try {
                long targetId = Long.parseLong(query.get("id").toString().trim());
                return list.stream()
                        .filter(c -> Objects.equals(c.getId(), targetId))
                        .findFirst()
                        .orElse(null);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // 2. 批量案例 ID 列表过滤
        if (query.containsKey("ids")) {
            Object idsObj = query.get("ids");
            List<Long> targetIds = parseLongList(idsObj);
            if (!targetIds.isEmpty()) {
                list = list.stream()
                        .filter(c -> targetIds.contains(c.getId()))
                        .collect(Collectors.toList());
            }
        }

        // 3. limit / pageSize 数量截取
        int limit = getLimitFromQuery(query);
        if (limit > 0 && list.size() > limit) {
            return list.stream().limit(limit).collect(Collectors.toList());
        }

        return list;
    }

    private List<Long> parseLongList(Object val) {
        if (val == null) {
            return Collections.emptyList();
        }
        if (val instanceof List) {
            List<?> list = (List<?>) val;
            return list.stream()
                    .map(o -> {
                        try {
                            return Long.parseLong(o.toString().trim());
                        } catch (NumberFormatException e) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private int getLimitFromQuery(Map<String, Object> query) {
        Object limitObj = query.get("limit");
        if (limitObj == null) {
            limitObj = query.get("pageSize");
        }
        if (limitObj != null) {
            try {
                return Integer.parseInt(limitObj.toString().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
