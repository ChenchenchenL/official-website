package com.company.officialwebsite.modules.pagebuilder.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.PublishSnapshotStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.impl.PageDiffServiceImpl;
import com.company.officialwebsite.modules.pagebuilder.vo.PublishReviewVO;
import com.company.officialwebsite.modules.pagebuilder.vo.SchemaDiffItemVO;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class PageDiffServiceTest {

    @Mock
    private PageDefinitionMapper pageDefinitionMapper;
    @Mock
    private PageDraftMapper pageDraftMapper;
    @Mock
    private PagePublishSnapshotMapper pagePublishSnapshotMapper;
    @Mock
    private PageSchemaValidationService pageSchemaValidationService;

    private PageDiffService pageDiffService;

    @BeforeEach
    void setUp() {
        pageDiffService = new PageDiffServiceImpl(
                pageDefinitionMapper, pageDraftMapper, pagePublishSnapshotMapper, pageSchemaValidationService
        );
    }

    @Test
    void comparePageSchema_shouldReturnAllAdded_whenNoActiveSnapshotExists() {
        Long pageId = 100L;

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(pageId);
        draft.setVersion(1);
        draft.setSchemaHash("hash_draft_1");

        PageSchemaModel draftSchema = new PageSchemaModel();
        SectionModel hero = new SectionModel();
        hero.setId("sec_hero");
        hero.setComponent("HeroBanner");
        draftSchema.setSections(List.of(hero));

        draft.setSchemaJson(draftSchema);

        when(pageDraftMapper.selectOne(any())).thenReturn(draft);
        when(pagePublishSnapshotMapper.selectOne(any())).thenReturn(null);

        List<SchemaDiffItemVO> diffs = pageDiffService.comparePageSchema(pageId, null);

        Assertions.assertNotNull(diffs);
        Assertions.assertEquals(1, diffs.size());
        Assertions.assertEquals("ADDED", diffs.get(0).getChangeType());
        Assertions.assertEquals("sec_hero", diffs.get(0).getComponentId());
    }

    @Test
    void comparePageSchema_shouldMaskSensitiveKeys() {
        Long pageId = 100L;

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(pageId);

        PageSchemaModel oldSchema = new PageSchemaModel();
        SectionModel oldSec = new SectionModel();
        oldSec.setId("sec_1");
        oldSec.setComponent("Text");
        oldSec.setProps(Map.of("secretToken", "123456"));
        oldSchema.setSections(List.of(oldSec));

        PageSchemaModel newSchema = new PageSchemaModel();
        SectionModel newSec = new SectionModel();
        newSec.setId("sec_1");
        newSec.setComponent("Text");
        newSec.setProps(Map.of("secretToken", "888888"));
        newSchema.setSections(List.of(newSec));

        draft.setSchemaJson(newSchema);

        PagePublishSnapshotEntity activeSnapshot = new PagePublishSnapshotEntity();
        activeSnapshot.setSnapshotJson(oldSchema);

        when(pageDraftMapper.selectOne(any())).thenReturn(draft);
        when(pagePublishSnapshotMapper.selectOne(any())).thenReturn(activeSnapshot);

        List<SchemaDiffItemVO> diffs = pageDiffService.comparePageSchema(pageId, null);

        Assertions.assertNotNull(diffs);
        Assertions.assertEquals(1, diffs.size());
        Assertions.assertEquals("MODIFIED", diffs.get(0).getChangeType());
        Assertions.assertEquals("******", diffs.get(0).getOldValue());
        Assertions.assertEquals("******", diffs.get(0).getNewValue());
    }

    @Test
    void generatePublishReview_shouldExtractBindingSourcesAndValidationResult() {
        Long pageId = 100L;

        PageDefinitionEntity pageDef = new PageDefinitionEntity();
        pageDef.setId(pageId);
        pageDef.setName("Home Page");

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(pageId);
        draft.setVersion(2);
        draft.setSchemaHash("hash_draft_2");

        PageSchemaModel draftSchema = new PageSchemaModel();
        SectionModel sec1 = new SectionModel();
        sec1.setId("sec_1");
        sec1.setComponent("ProductList");
        BindingModel b1 = new BindingModel();
        b1.setSource("product");
        sec1.setBinding(b1);

        SectionModel sec2 = new SectionModel();
        sec2.setId("sec_2");
        sec2.setComponent("CaseList");
        BindingModel b2 = new BindingModel();
        b2.setSource("case");
        sec2.setBinding(b2);

        draftSchema.setSections(List.of(sec1, sec2));
        draft.setSchemaJson(draftSchema);

        when(pageDefinitionMapper.selectById(pageId)).thenReturn(pageDef);
        when(pageDraftMapper.selectOne(any())).thenReturn(draft);
        when(pagePublishSnapshotMapper.selectOne(any())).thenReturn(null);

        doNothing().when(pageSchemaValidationService).validateSchema(any());

        PublishReviewVO review = pageDiffService.generatePublishReview(pageId);

        Assertions.assertNotNull(review);
        Assertions.assertEquals("Home Page", review.getPageName());
        Assertions.assertTrue(review.isValidationPassed());
        Assertions.assertNull(review.getValidationErrorMessage());
        Assertions.assertEquals(2, review.getDraftSectionCount());
        Assertions.assertTrue(review.getBindingSources().contains("product"));
        Assertions.assertTrue(review.getBindingSources().contains("case"));
    }

    @Test
    void comparePageSchema_shouldDetectModifiedFields() {
        Long pageId = 100L;

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(pageId);

        PageSchemaModel oldSchema = new PageSchemaModel();
        SectionModel oldSec = new SectionModel();
        oldSec.setId("sec_1");
        oldSec.setComponent("Heading");
        oldSec.setProps(Map.of("text", "Old Title"));
        BindingModel oldB = new BindingModel();
        oldB.setSource("product");
        oldSec.setBinding(oldB);
        oldSchema.setSections(List.of(oldSec));

        PageSchemaModel newSchema = new PageSchemaModel();
        SectionModel newSec = new SectionModel();
        newSec.setId("sec_1");
        newSec.setComponent("Heading");
        newSec.setProps(Map.of("text", "New Title"));
        BindingModel newB = new BindingModel();
        newB.setSource("case");
        newSec.setBinding(newB);
        newSchema.setSections(List.of(newSec));

        draft.setSchemaJson(newSchema);

        PagePublishSnapshotEntity activeSnapshot = new PagePublishSnapshotEntity();
        activeSnapshot.setSnapshotJson(oldSchema);

        when(pageDraftMapper.selectOne(any())).thenReturn(draft);
        when(pagePublishSnapshotMapper.selectOne(any())).thenReturn(activeSnapshot);

        List<SchemaDiffItemVO> diffs = pageDiffService.comparePageSchema(pageId, null);

        Assertions.assertNotNull(diffs);
        Assertions.assertEquals(2, diffs.size());
        Assertions.assertTrue(diffs.stream().anyMatch(d -> "MODIFIED".equals(d.getChangeType()) && "text".equals(d.getFieldName())));
        Assertions.assertTrue(diffs.stream().anyMatch(d -> "MODIFIED".equals(d.getChangeType()) && "source".equals(d.getFieldName())));
    }

    @Test
    void generatePublishReview_shouldSetValidationPassedFalse_whenValidationFails() {
        Long pageId = 100L;

        PageDefinitionEntity pageDef = new PageDefinitionEntity();
        pageDef.setId(pageId);
        pageDef.setName("Home Page");

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(pageId);
        draft.setVersion(2);
        draft.setSchemaJson(new PageSchemaModel());

        when(pageDefinitionMapper.selectById(pageId)).thenReturn(pageDef);
        when(pageDraftMapper.selectOne(any())).thenReturn(draft);
        when(pagePublishSnapshotMapper.selectOne(any())).thenReturn(null);

        doThrow(new BusinessException(com.company.officialwebsite.common.enums.ErrorCode.COMMON_PARAM_INVALID, "zIndex 超出允许范围"))
                .when(pageSchemaValidationService).validateSchema(any());

        PublishReviewVO review = pageDiffService.generatePublishReview(pageId);

        Assertions.assertNotNull(review);
        Assertions.assertFalse(review.isValidationPassed());
        Assertions.assertEquals("zIndex 超出允许范围", review.getValidationErrorMessage());
    }

    @Test
    void comparePageSchema_shouldThrow_whenCompareVersionNotFound() {
        Long pageId = 100L;

        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(pageId);
        when(pageDraftMapper.selectOne(any())).thenReturn(draft);
        when(pagePublishSnapshotMapper.selectOne(any())).thenReturn(null);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> pageDiffService.comparePageSchema(pageId, 999L)
        );

        Assertions.assertEquals(com.company.officialwebsite.common.enums.ErrorCode.COMMON_RESOURCE_NOT_FOUND, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("对比的版本快照不存在"));
    }
}
