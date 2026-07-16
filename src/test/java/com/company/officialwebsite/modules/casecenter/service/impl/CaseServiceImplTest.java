package com.company.officialwebsite.modules.casecenter.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.casecenter.converter.CaseConverter;
import com.company.officialwebsite.modules.casecenter.dto.CaseCreateDTO;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.mapper.CaseMapper;
import com.company.officialwebsite.modules.casecenter.mapper.CaseVersionMapper;
import com.company.officialwebsite.modules.casecenter.vo.AdminCaseVO;
import com.company.officialwebsite.modules.content.mapper.ContentRelationMapper;
import com.company.officialwebsite.modules.content.service.ContentReferenceGuard;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.product.converter.ProductConverter;
import com.company.officialwebsite.modules.product.mapper.ProductMapper;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class CaseServiceImplTest {

    @Mock
    private CaseMapper caseMapper;

    @Mock
    private CaseVersionMapper caseVersionMapper;

    @Mock
    private CaseConverter caseConverter;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductConverter productConverter;

    @Mock
    private ContentRelationMapper contentRelationMapper;

    @Mock
    private MediaAssetService mediaAssetService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private PortalCacheInvalidationSupport portalCacheInvalidationSupport;

    @Mock
    private PortalCacheKeyBuilder portalCacheKeyBuilder;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private ContentReferenceGuard contentReferenceGuard;

    private CaseServiceImpl service;

    @BeforeEach
    void setUp() {
        OfficialProperties properties = new OfficialProperties();
        properties.getCache().setDefaultTtl(Duration.ofMinutes(10));
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new CaseServiceImpl(
                caseMapper,
                caseVersionMapper,
                caseConverter,
                productMapper,
                productConverter,
                contentRelationMapper,
                mediaAssetService,
                auditLogService,
                properties,
                new PortalCacheSupport(redisTemplate, portalCacheKeyBuilder, portalCacheInvalidationSupport, properties, objectMapper),
                org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class),
                contentReferenceGuard,
                objectMapper);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(portalCacheKeyBuilder.build(anyString())).thenReturn("official:portal:cases");
    }

    @Test
    void createCase_shouldRejectBlankTitle() {
        CaseCreateDTO dto = new CaseCreateDTO();
        dto.setTitle(" ");
        dto.setLogoMediaId(1L);
        dto.setSummary("摘要");
        dto.setKeywords(Collections.singletonList("标签"));
        dto.setVisible(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createCase(dto));
        assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void createCase_shouldConvertDuplicateTitleToBusinessException() {
        CaseCreateDTO dto = new CaseCreateDTO();
        dto.setTitle("案例A");
        dto.setLogoMediaId(1L);
        dto.setSummary("摘要");
        dto.setKeywords(List.of("标签"));
        dto.setVisible(true);

        MediaAssetEntity asset = new MediaAssetEntity();
        asset.setId(1L);
        when(mediaAssetService.requireUsableImage(1L)).thenReturn(asset);
        when(caseMapper.insert(any(CaseEntity.class))).thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createCase(dto));
        assertEquals(ErrorCode.CASE_TITLE_DUPLICATE, ex.getErrorCode());
        verify(mediaAssetService).requireUsableImage(1L);
    }

    @Test
    void createCase_shouldRejectNullLogoWithBusinessException() {
        CaseCreateDTO dto = new CaseCreateDTO();
        dto.setTitle("案例A");
        dto.setLogoMediaId(null);
        dto.setSummary("摘要");
        dto.setKeywords(List.of("标签"));
        dto.setVisible(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createCase(dto));
        assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void createCase_shouldWrapLogoResolutionFailure() {
        CaseCreateDTO dto = new CaseCreateDTO();
        dto.setTitle("案例A");
        dto.setLogoMediaId(1L);
        dto.setSummary("摘要");
        dto.setKeywords(List.of("标签"));
        dto.setVisible(true);

        when(mediaAssetService.requireUsableImage(1L))
                .thenThrow(new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "missing"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createCase(dto));
        assertEquals(ErrorCode.CASE_LOGO_INVALID, ex.getErrorCode());
        verify(mediaAssetService).requireUsableImage(1L);
    }

    @Test
    void getAdminCaseList_shouldReturnPageResult() {
        Page<CaseEntity> page = new Page<>(1, 20);
        page.setTotal(0);
        page.setRecords(List.of());
        when(caseMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<AdminCaseVO> result = service.getAdminCaseList(1, 20);

        assertEquals(0, result.getTotal());
        assertEquals(0, result.getList().size());
    }

    @Test
    void getPortalCases_shouldReturnEmptyList_whenCacheAndDbEmpty() {
        when(valueOperations.get(any())).thenReturn(null);
        when(caseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getPortalCases());
    }
}
