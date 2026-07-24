package com.company.officialwebsite.modules.pagebuilder.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftHistoryEntity;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftHistoryMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.service.impl.PageDraftHistoryServiceImpl;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftHistoryVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class PageDraftHistoryServiceTest {

    @Mock
    private PageDraftHistoryMapper historyMapper;
    @Mock
    private EditorLockService lockService;
    @Mock
    private PageDraftService draftService;

    private ObjectMapper objectMapper = new ObjectMapper();
    private PageDraftHistoryService historyService;

    @BeforeEach
    void setUp() {
        historyService = new PageDraftHistoryServiceImpl(historyMapper, lockService, draftService, objectMapper);
    }

    @Test
    void recordRevision_shouldInsertAndPrune_whenCountExceeds20() {
        Long pageId = 100L;
        Long draftId = 1L;
        String schemaJsonStr = "{\"schemaVersion\":1}";
        String schemaHash = "hash123";

        when(historyMapper.selectOne(any())).thenReturn(null); // revision_no = 1

        List<PageDraftHistoryEntity> mockEntities = new ArrayList<>();
        for (int i = 21; i >= 1; i--) {
            PageDraftHistoryEntity entity = new PageDraftHistoryEntity();
            entity.setId((long) i);
            entity.setRevisionNo(i);
            mockEntities.add(entity);
        }
        when(historyMapper.selectList(any())).thenReturn(mockEntities);

        historyService.recordRevision(pageId, draftId, schemaJsonStr, schemaHash, "test remark", "admin");

        verify(historyMapper).insert(any(PageDraftHistoryEntity.class));
        verify(historyMapper).deleteBatchIds(any());
    }

    @Test
    void getRevisionsPage_shouldReturnPagedVOs() {
        Long pageId = 100L;
        PageDraftHistoryEntity entity = new PageDraftHistoryEntity();
        entity.setId(1L);
        entity.setPageId(pageId);
        entity.setRevisionNo(1);
        entity.setSchemaJson("{\"name\":\"Test Page\"}");

        Page<PageDraftHistoryEntity> pageResult = new Page<>(1, 10);
        pageResult.setRecords(List.of(entity));
        pageResult.setTotal(1);

        when(historyMapper.selectPage(any(), any())).thenReturn(pageResult);

        PageResult<PageDraftHistoryVO> result = historyService.getRevisionsPage(pageId, 1, 10);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.getTotal());
        Assertions.assertNull(result.getList().get(0).getSchemaJson()); // 摘要列表中排除 schemaJson
    }

    @Test
    void getRevisionDetail_shouldReturnFullVO_whenFound() {
        Long pageId = 100L;
        Long revisionId = 5L;

        PageDraftHistoryEntity entity = new PageDraftHistoryEntity();
        entity.setId(revisionId);
        entity.setPageId(pageId);
        entity.setRevisionNo(5);
        entity.setSchemaJson("{\"schemaVersion\":1,\"name\":\"Full Schema Page\"}");

        when(historyMapper.selectById(revisionId)).thenReturn(entity);

        PageDraftHistoryVO vo = historyService.getRevisionDetail(pageId, revisionId);

        Assertions.assertNotNull(vo);
        Assertions.assertNotNull(vo.getSchemaJson());
        Assertions.assertEquals("Full Schema Page", vo.getSchemaJson().getName());
    }

    @Test
    void restoreRevision_shouldInvokeSaveDraft_whenValid() {
        Long pageId = 100L;
        Long revisionId = 5L;
        Integer version = 3;
        String lockToken = "token_abc";

        PageDraftHistoryEntity entity = new PageDraftHistoryEntity();
        entity.setId(revisionId);
        entity.setPageId(pageId);
        entity.setRevisionNo(5);
        entity.setSchemaJson("{\"schemaVersion\":1,\"name\":\"Restored Page\"}");

        when(historyMapper.selectById(revisionId)).thenReturn(entity);

        PageDraftVO mockUpdatedVO = new PageDraftVO();
        mockUpdatedVO.setId(10L);
        mockUpdatedVO.setPageId(pageId);
        mockUpdatedVO.setVersion(4);

        when(draftService.saveDraft(eq(pageId), any(PageSchemaModel.class), anyString(), eq(version), eq(lockToken), any()))
                .thenReturn(mockUpdatedVO);

        PageDraftVO result = historyService.restoreRevision(pageId, revisionId, version, lockToken);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(4, result.getVersion());
        verify(lockService).validateLockForEdit(eq(pageId), eq(lockToken), any());
    }
}
