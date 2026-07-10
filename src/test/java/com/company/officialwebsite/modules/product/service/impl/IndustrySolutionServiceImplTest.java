package com.company.officialwebsite.modules.product.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.infrastructure.cache.PortalCacheInvalidationSupport;
import com.company.officialwebsite.infrastructure.cache.PortalCacheKeyBuilder;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.content.service.ContentReferenceGuard;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.product.converter.IndustrySolutionConverter;
import com.company.officialwebsite.modules.product.dto.IndustrySolutionCreateDTO;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionEntity;
import com.company.officialwebsite.modules.product.mapper.IndustrySolutionMapper;
import com.company.officialwebsite.modules.product.vo.AdminIndustrySolutionVO;
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
class IndustrySolutionServiceImplTest {

    @Mock
    private IndustrySolutionMapper industrySolutionMapper;

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
    private IndustrySolutionConverter industrySolutionConverter;

    @Mock
    private ContentReferenceGuard contentReferenceGuard;

    private OfficialProperties officialProperties;

    private IndustrySolutionServiceImpl service;

    @BeforeEach
    void setUp() {
        officialProperties = new OfficialProperties();
        officialProperties.getCache().setDefaultTtl(Duration.ofMinutes(10));
        service = new IndustrySolutionServiceImpl(
                industrySolutionMapper,
                industrySolutionConverter,
                mediaAssetService,
                auditLogService,
                officialProperties,
                new PortalCacheSupport(redisTemplate, portalCacheKeyBuilder, portalCacheInvalidationSupport, officialProperties, new ObjectMapper().registerModule(new JavaTimeModule())),
                org.mockito.Mockito.mock(org.springframework.context.ApplicationEventPublisher.class),
                contentReferenceGuard);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(portalCacheKeyBuilder.build(anyString())).thenReturn("official:portal:industry_solutions");
    }

    @Test
    void createIndustrySolution_shouldRejectNullIconWithBusinessException() {
        IndustrySolutionCreateDTO dto = new IndustrySolutionCreateDTO();
        dto.setName("行业A");
        dto.setIconMediaId(null);
        dto.setDescription("说明");
        dto.setCustomerTags(Collections.singletonList("客户A"));
        dto.setVisible(true);

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createIndustrySolution(dto));
        assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
    }

    @Test
    void createIndustrySolution_shouldConvertDuplicateNameToBusinessException() {
        IndustrySolutionCreateDTO dto = new IndustrySolutionCreateDTO();
        dto.setName("行业A");
        dto.setIconMediaId(1L);
        dto.setDescription("说明");
        dto.setCustomerTags(Collections.singletonList("客户A"));
        dto.setVisible(true);

        MediaAssetEntity asset = new MediaAssetEntity();
        asset.setId(1L);
        when(mediaAssetService.requireUsableImage(1L)).thenReturn(asset);
        when(industrySolutionMapper.insert(any(IndustrySolutionEntity.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("duplicate"));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.createIndustrySolution(dto));
        assertEquals(ErrorCode.PRODUCT_SOLUTION_NAME_DUPLICATE, ex.getErrorCode());
        verify(mediaAssetService).requireUsableImage(1L);
    }

    @Test
    void requireActiveIndustrySolution_shouldThrowNotFoundBusinessException() {
        when(industrySolutionMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class, () ->
                service.updateIndustrySolution(99L, buildUpdateDto()));
        assertEquals(ErrorCode.PRODUCT_SOLUTION_NOT_FOUND, ex.getErrorCode());
    }

    @Test
    void getAdminIndustrySolutionList_shouldReturnPageResult() {
        Page<IndustrySolutionEntity> page = new Page<>(1, 20);
        page.setTotal(0);
        page.setRecords(List.of());
        when(industrySolutionMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class))).thenReturn(page);

        PageResult<AdminIndustrySolutionVO> result = service.getAdminIndustrySolutionList(1, 20);

        assertEquals(0, result.getTotal());
        assertEquals(0, result.getList().size());
    }

    @Test
    void getPortalIndustrySolutions_shouldReturnEmptyList_whenCacheAndDbEmpty() {
        when(valueOperations.get(any())).thenReturn(null);
        when(industrySolutionMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());

        assertDoesNotThrow(() -> service.getPortalIndustrySolutions());
    }

    private com.company.officialwebsite.modules.product.dto.IndustrySolutionUpdateDTO buildUpdateDto() {
        com.company.officialwebsite.modules.product.dto.IndustrySolutionUpdateDTO dto =
                new com.company.officialwebsite.modules.product.dto.IndustrySolutionUpdateDTO();
        dto.setName("行业A");
        dto.setIconMediaId(1L);
        dto.setDescription("说明");
        dto.setCustomerTags(List.of("客户A"));
        dto.setVisible(true);
        dto.setVersion(0);
        return dto;
    }
}
