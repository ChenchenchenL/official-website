package com.company.officialwebsite.modules.pagebuilder.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.converter.ComponentTemplateConverter;
import com.company.officialwebsite.modules.pagebuilder.entity.ComponentTemplateEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.ComponentTemplateStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.ComponentTemplateMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.impl.ComponentTemplateServiceImpl;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateUsageVO;
import com.company.officialwebsite.modules.pagebuilder.converter.PageDefinitionConverter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ComponentTemplateServiceTest：验证组件物料模板管理业务服务逻辑。
 */
@ExtendWith(MockitoExtension.class)
class ComponentTemplateServiceTest {

    @Mock
    private ComponentTemplateMapper templateMapper;

    @Mock
    private PageDraftMapper pageDraftMapper;

    @Mock
    private PagePublishSnapshotMapper pagePublishSnapshotMapper;

    @Mock
    private PageDefinitionMapper pageDefinitionMapper;

    private final ComponentTemplateConverter templateConverter = new ComponentTemplateConverter();
    private final PageDefinitionConverter pageDefinitionConverter = new PageDefinitionConverter();

    private ComponentTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new ComponentTemplateServiceImpl(
                templateMapper,
                templateConverter,
                pageDraftMapper,
                pagePublishSnapshotMapper,
                pageDefinitionMapper,
                pageDefinitionConverter);
    }

    @Test
    void getActiveTemplates_shouldReturnList() {
        ComponentTemplateEntity entity = new ComponentTemplateEntity();
        entity.setId(1L);
        entity.setComponentCode("HeroBanner");
        entity.setStatus(ComponentTemplateStatusEnum.ACTIVE.name());

        when(templateMapper.selectList(any())).thenReturn(Collections.singletonList(entity));

        List<ComponentTemplateVO> result = templateService.getActiveTemplates();
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("HeroBanner", result.get(0).getComponentCode());
    }

    @Test
    void getTemplateByCode_shouldReturnDetails_whenExists() {
        ComponentTemplateEntity entity = new ComponentTemplateEntity();
        entity.setId(1L);
        entity.setComponentCode("HeroBanner");
        entity.setStatus(ComponentTemplateStatusEnum.ACTIVE.name());

        when(templateMapper.selectOne(any())).thenReturn(entity);

        ComponentTemplateVO result = templateService.getTemplateByCode("HeroBanner");
        Assertions.assertNotNull(result);
        Assertions.assertEquals("HeroBanner", result.getComponentCode());
    }

    @Test
    void getTemplateByCode_shouldThrowNotFound_whenNotExists() {
        when(templateMapper.selectOne(any())).thenReturn(null);

        BusinessException exception = Assertions.assertThrows(
                BusinessException.class,
                () -> templateService.getTemplateByCode("InvalidCode")
        );
        Assertions.assertEquals(ErrorCode.COMMON_RESOURCE_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    void getTemplateUsage_shouldSeparateActiveSnapshotAndDraftPages() {
        ComponentTemplateEntity template = new ComponentTemplateEntity();
        template.setComponentCode("HeroBanner");
        when(templateMapper.selectOne(any())).thenReturn(template);

        PagePublishSnapshotEntity snapshot = new PagePublishSnapshotEntity();
        snapshot.setPageId(1L);
        snapshot.setSnapshotJson(schemaWithComponent("HeroBanner"));
        when(pagePublishSnapshotMapper.selectList(any())).thenReturn(List.of(snapshot));

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(2L);
        draft.setSchemaJson(schemaWithComponent("HeroBanner"));
        when(pageDraftMapper.selectList(any())).thenReturn(List.of(draft));

        PageDefinitionEntity activePage = page(1L, "home");
        PageDefinitionEntity draftPage = page(2L, "about");
        when(pageDefinitionMapper.selectBatchIds(any())).thenReturn(List.of(activePage, draftPage));

        ComponentTemplateUsageVO usage = templateService.getTemplateUsage("HeroBanner", 1, 20);

        Assertions.assertEquals("HeroBanner", usage.getComponentCode());
        Assertions.assertEquals(1L, usage.getActiveSnapshotPages().getTotal());
        Assertions.assertEquals(1L, usage.getDraftPages().getTotal());
        Assertions.assertEquals("home", usage.getActiveSnapshotPages().getList().get(0).getPageKey());
        Assertions.assertEquals("about", usage.getDraftPages().getList().get(0).getPageKey());
    }

    private PageSchemaModel schemaWithComponent(String componentCode) {
        SectionModel section = new SectionModel();
        section.setComponent(componentCode);
        PageSchemaModel schema = new PageSchemaModel();
        schema.setSections(List.of(section));
        return schema;
    }

    private PageDefinitionEntity page(Long id, String pageKey) {
        PageDefinitionEntity page = new PageDefinitionEntity();
        page.setId(id);
        page.setPageKey(pageKey);
        page.setName(pageKey);
        page.setRoutePath("/" + pageKey);
        return page;
    }
}
