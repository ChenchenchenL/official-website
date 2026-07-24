package com.company.officialwebsite.application.portal;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.officialwebsite.application.portal.impl.PageBindingResolutionServiceImpl;
import com.company.officialwebsite.modules.casecenter.service.CaseService;
import com.company.officialwebsite.modules.lead.service.ContactInfoService;
import com.company.officialwebsite.modules.lead.service.CooperationDirectionTagService;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.product.service.IndustrySolutionService;
import com.company.officialwebsite.modules.product.service.ProductService;
import com.company.officialwebsite.modules.site.service.HomeMetricCardService;
import com.company.officialwebsite.modules.site.service.NavigationMenuService;
import com.company.officialwebsite.modules.site.service.SiteConfigService;
import com.company.officialwebsite.modules.site.service.TimelineEventService;
import com.company.officialwebsite.modules.site.service.ValueCardService;
import com.company.officialwebsite.modules.site.vo.PortalSiteConfigVO;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * PageBindingResolutionServiceTest：验证应用层绑定解析服务对各业务模块数据源的正确路由能力。
 */
@ExtendWith(MockitoExtension.class)
class PageBindingResolutionServiceTest {

    @Mock
    private SiteConfigService siteConfigService;
    @Mock
    private NavigationMenuService navigationMenuService;
    @Mock
    private HomeMetricCardService homeMetricCardService;
    @Mock
    private ProductService productService;
    @Mock
    private IndustrySolutionService industrySolutionService;
    @Mock
    private CaseService caseService;
    @Mock
    private TimelineEventService timelineEventService;
    @Mock
    private ValueCardService valueCardService;
    @Mock
    private PromisePortalApplicationService promisePortalApplicationService;
    @Mock
    private ContactInfoService contactInfoService;
    @Mock
    private CooperationDirectionTagService cooperationDirectionTagService;

    private PageBindingResolutionService service;

    @BeforeEach
    void setUp() {
        service = new PageBindingResolutionServiceImpl(
                siteConfigService, navigationMenuService, homeMetricCardService,
                productService, industrySolutionService, caseService,
                timelineEventService, valueCardService, promisePortalApplicationService,
                contactInfoService, cooperationDirectionTagService);
    }

    /**
     * STATIC 绑定模式应直接返回 null（不调用任何模块 Service）。
     */
    @Test
    void resolveBinding_shouldReturnNull_whenModeIsStatic() {
        BindingModel binding = new BindingModel();
        binding.setMode("STATIC");
        binding.setSource("site_config");

        Object result = service.resolveBinding(binding);

        assertNull(result);
    }

    /**
     * binding 为 null 时，应返回 null，不抛出异常。
     */
    @Test
    void resolveBinding_shouldReturnNull_whenBindingIsNull() {
        assertNull(service.resolveBinding(null));
    }

    /**
     * source=site_config 时，应调用 SiteConfigService.getPortalConfig()。
     */
    @Test
    void resolveBinding_shouldCallSiteConfigService_whenSourceIsSiteConfig() {
        PortalSiteConfigVO mockConfig = new PortalSiteConfigVO();
        when(siteConfigService.getPortalConfig()).thenReturn(mockConfig);

        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("site_config");

        Object result = service.resolveBinding(binding);

        assertNotNull(result);
        verify(siteConfigService).getPortalConfig();
    }

    /**
     * source=navigation_menu 时，应调用 NavigationMenuService.getPortalMenuTree()。
     */
    @Test
    void resolveBinding_shouldCallNavigationMenuService_whenSourceIsNavigationMenu() {
        when(navigationMenuService.getPortalMenuTree()).thenReturn(Collections.emptyList());

        BindingModel binding = new BindingModel();
        binding.setMode("AGGREGATE");
        binding.setSource("navigation_menu");

        service.resolveBinding(binding);

        verify(navigationMenuService).getPortalMenuTree();
    }

    /**
     * source=product 时，应调用 ProductService.getPortalProducts()。
     */
    @Test
    void resolveBinding_shouldCallProductService_whenSourceIsProduct() {
        when(productService.getPortalProducts()).thenReturn(Collections.emptyList());

        BindingModel binding = new BindingModel();
        binding.setMode("AGGREGATE");
        binding.setSource("product");

        service.resolveBinding(binding);

        verify(productService).getPortalProducts();
    }

    /**
     * source=case 时，应调用 CaseService.getPortalCases()。
     */
    @Test
    void resolveBinding_shouldCallCaseService_whenSourceIsCase() {
        when(caseService.getPortalCases()).thenReturn(Collections.emptyList());

        BindingModel binding = new BindingModel();
        binding.setMode("AGGREGATE");
        binding.setSource("case");

        service.resolveBinding(binding);

        verify(caseService).getPortalCases();
    }

    /**
     * source=contact_info 时，应调用 ContactInfoService.getPortalContactInfo()。
     */
    @Test
    void resolveBinding_shouldCallContactInfoService_whenSourceIsContactInfo() {
        when(contactInfoService.getPortalContactInfo()).thenReturn(null);

        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("contact_info");

        service.resolveBinding(binding);

        verify(contactInfoService).getPortalContactInfo();
    }

    /**
     * 不合法的 source 值应抛出 BusinessException（不支持的绑定源）。
     * 注意：PageSchemaValidationService 在保存草稿时已拦截此情况；
     * 此处验证 resolveBinding 的防御性处理。
     */
    @Test
    void resolveBinding_shouldThrowBusinessException_whenSourceIsInvalid() {
        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("invalid_source");

        org.junit.jupiter.api.Assertions.assertThrows(
                com.company.officialwebsite.common.exception.BusinessException.class,
                () -> service.resolveBinding(binding));
    }

    @Test
    void resolveBinding_shouldFilterProductById_whenQueryContainsId() {
        com.company.officialwebsite.modules.product.vo.PortalProductVO p1 = new com.company.officialwebsite.modules.product.vo.PortalProductVO();
        p1.setId(100L);
        p1.setName("Product 100");

        com.company.officialwebsite.modules.product.vo.PortalProductVO p2 = new com.company.officialwebsite.modules.product.vo.PortalProductVO();
        p2.setId(200L);
        p2.setName("Product 200");

        when(productService.getPortalProducts()).thenReturn(java.util.List.of(p1, p2));

        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("product");
        binding.setQuery(java.util.Map.of("id", "100"));

        Object result = service.resolveBinding(binding);

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertEquals(p1, result);
    }

    @Test
    void resolveBinding_shouldFilterCasesByIdsAndLimit_whenQueryContainsIdsAndLimit() {
        com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO c1 = new com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO();
        c1.setId(1L);

        com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO c2 = new com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO();
        c2.setId(2L);

        when(caseService.getPortalCases()).thenReturn(java.util.List.of(c1, c2));

        BindingModel binding = new BindingModel();
        binding.setMode("AGGREGATE");
        binding.setSource("case");
        binding.setQuery(java.util.Map.of("ids", java.util.List.of(1L, 2L), "limit", 1));

        Object result = service.resolveBinding(binding);

        assertNotNull(result);
        org.junit.jupiter.api.Assertions.assertTrue(result instanceof java.util.List);
        java.util.List<?> list = (java.util.List<?>) result;
        org.junit.jupiter.api.Assertions.assertEquals(1, list.size());
    }
}
