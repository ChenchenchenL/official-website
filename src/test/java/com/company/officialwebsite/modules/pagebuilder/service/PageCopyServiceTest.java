package com.company.officialwebsite.modules.pagebuilder.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.converter.PageDefinitionConverter;
import com.company.officialwebsite.modules.pagebuilder.dto.PageCopyDTO;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.impl.PageCopyServiceImpl;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDefinitionVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class PageCopyServiceTest {

    @Mock
    private PageDefinitionMapper pageDefinitionMapper;
    @Mock
    private PageDraftMapper pageDraftMapper;
    @Mock
    private PagePublishSnapshotMapper pagePublishSnapshotMapper;
    @Mock
    private PageDraftService pageDraftService;

    private PageDefinitionConverter pageDefinitionConverter = new PageDefinitionConverter();
    private ObjectMapper objectMapper = new ObjectMapper();

    private PageCopyService pageCopyService;

    @BeforeEach
    void setUp() {
        pageCopyService = new PageCopyServiceImpl(
                pageDefinitionMapper, pageDraftMapper, pagePublishSnapshotMapper, pageDraftService, pageDefinitionConverter, objectMapper
        );
    }

    @Test
    void copyPage_shouldSuccess_andReassignSectionIds() {
        Long sourcePageId = 10L;

        PageDraftEntity sourceDraft = new PageDraftEntity();
        sourceDraft.setPageId(sourcePageId);

        PageSchemaModel sourceSchema = new PageSchemaModel();
        SectionModel sec = new SectionModel();
        sec.setId("old_sec_100");
        sec.setComponent("HeroBanner");
        sourceSchema.setSections(List.of(sec));
        sourceDraft.setSchemaJson(sourceSchema);

        PageDefinitionEntity sourcePageDef = new PageDefinitionEntity();
        sourcePageDef.setId(sourcePageId);
        when(pageDefinitionMapper.selectById(sourcePageId)).thenReturn(sourcePageDef);
        when(pageDefinitionMapper.selectCount(any())).thenReturn(0L);
        when(pageDraftMapper.selectOne(any())).thenReturn(sourceDraft);

        PageCopyDTO dto = new PageCopyDTO();
        dto.setSourcePageId(sourcePageId);
        dto.setTargetName("Copied Page");
        dto.setTargetPath("copied-path"); // 未带前斜杠，校验自动归一化
        dto.setTargetPageKey("copied_key");

        doAnswer(invocation -> {
            PageDefinitionEntity entity = invocation.getArgument(0);
            entity.setId(200L);
            return 1;
        }).when(pageDefinitionMapper).insert(any(PageDefinitionEntity.class));

        PageDefinitionVO result = pageCopyService.copyPage(dto, "adminUser");

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Copied Page", result.getName());
        Assertions.assertEquals("/copied-path", result.getRoutePath()); // 验证自动归一化 / 前缀
        Assertions.assertEquals("copied_key", result.getPageKey());
        Assertions.assertEquals(sourcePageId, result.getSourcePageId());

        // 验证调用了 pageDraftService.saveDraft，且 Schema 中 section id 被重新赋值
        ArgumentCaptor<PageSchemaModel> schemaCaptor = ArgumentCaptor.forClass(PageSchemaModel.class);
        verify(pageDraftService).saveDraft(eq(200L), schemaCaptor.capture(), anyString(), eq(0), any(), eq("adminUser"));

        PageSchemaModel targetSchema = schemaCaptor.getValue();
        Assertions.assertNotNull(targetSchema);
        Assertions.assertEquals(1, targetSchema.getSections().size());
        String newSecId = targetSchema.getSections().get(0).getId();
        Assertions.assertNotEquals("old_sec_100", newSecId);
        Assertions.assertTrue(newSecId.startsWith("sec_"));
    }

    @Test
    void copyPage_shouldThrow_whenSourcePageIsSoftDeleted() {
        when(pageDefinitionMapper.selectById(999L)).thenReturn(null);

        PageCopyDTO dto = new PageCopyDTO();
        dto.setSourcePageId(999L);
        dto.setTargetName("New Page");
        dto.setTargetPath("/new-path");
        dto.setTargetPageKey("new_key");

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> pageCopyService.copyPage(dto, "adminUser")
        );

        Assertions.assertTrue(ex.getMessage().contains("不存在或已被删除"));
    }

    @Test
    void copyPage_shouldThrow_whenPathOrKeyConflicts() {
        when(pageDefinitionMapper.selectCount(any())).thenReturn(1L);

        PageCopyDTO dto = new PageCopyDTO();
        dto.setSourcePageId(10L);
        dto.setTargetName("Dup Page");
        dto.setTargetPath("/existing-path");
        dto.setTargetPageKey("dup_key");

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> pageCopyService.copyPage(dto, "adminUser")
        );

        Assertions.assertTrue(ex.getMessage().contains("已被其他页面占用"));
    }

    @Test
    void copyPage_shouldThrow_whenSourcePageNotFound() {
        PageDefinitionEntity sourcePageDef = new PageDefinitionEntity();
        sourcePageDef.setId(999L);
        when(pageDefinitionMapper.selectById(999L)).thenReturn(sourcePageDef);
        when(pageDefinitionMapper.selectCount(any())).thenReturn(0L);
        when(pageDraftMapper.selectOne(any())).thenReturn(null);
        when(pagePublishSnapshotMapper.selectOne(any())).thenReturn(null);

        PageCopyDTO dto = new PageCopyDTO();
        dto.setSourcePageId(999L);
        dto.setTargetName("New Page");
        dto.setTargetPath("/new-path");
        dto.setTargetPageKey("new_key");

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> pageCopyService.copyPage(dto, "adminUser")
        );

        Assertions.assertTrue(ex.getMessage().contains("没有任何可用 Schema 数据"));
    }

    @Test
    void diagnoseSharedBlockImpact_shouldScanRefBlockId() {
        Long blockId = 88L;

        PageDraftEntity draft1 = new PageDraftEntity();
        draft1.setPageId(1L);
        PageSchemaModel schema1 = new PageSchemaModel();
        SectionModel sec1 = new SectionModel();
        sec1.setId("s1");
        sec1.setComponent("HeaderBlock");
        sec1.setBinding(new com.company.officialwebsite.modules.pagebuilder.model.BindingModel());
        sec1.getBinding().setQuery(Map.of("refBlockId", 88L));
        schema1.setSections(List.of(sec1));
        draft1.setSchemaJson(schema1);

        when(pageDraftMapper.selectList(any())).thenReturn(List.of(draft1));

        Map<String, Object> result = pageCopyService.diagnoseSharedBlockImpact(blockId);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(blockId, result.get("blockId"));
        Assertions.assertEquals(1, result.get("impactedCount"));
    }
}
