package com.company.officialwebsite.modules.pagebuilder.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.converter.ComponentTemplateConverter;
import com.company.officialwebsite.modules.pagebuilder.entity.ComponentTemplateEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.ComponentTemplateStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.ComponentTemplateMapper;
import com.company.officialwebsite.modules.pagebuilder.service.impl.ComponentTemplateServiceImpl;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

/**
 * ComponentTemplateServiceTest：验证组件物料模板管理业务服务逻辑。
 */
@ExtendWith(MockitoExtension.class)
class ComponentTemplateServiceTest {

    @Mock
    private ComponentTemplateMapper templateMapper;

    private final ComponentTemplateConverter templateConverter = new ComponentTemplateConverter();

    private ComponentTemplateService templateService;

    @BeforeEach
    void setUp() {
        templateService = new ComponentTemplateServiceImpl(templateMapper, templateConverter);
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
}
