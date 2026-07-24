package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.infrastructure.audit.AuditLog;
import com.company.officialwebsite.infrastructure.security.SecurityUtils;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftHistoryEntity;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftHistoryMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.service.EditorLockService;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftHistoryService;
import com.company.officialwebsite.modules.pagebuilder.service.PageDraftService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftHistoryVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PageDraftHistoryServiceImpl: 页面草稿历史修订服务实现类。
 */
@Service
public class PageDraftHistoryServiceImpl implements PageDraftHistoryService {

    private static final Logger log = LoggerFactory.getLogger(PageDraftHistoryServiceImpl.class);

    public static final int MAX_REVISIONS_PER_PAGE = 20;

    private final PageDraftHistoryMapper historyMapper;
    private final EditorLockService lockService;
    private final PageDraftService draftService;
    private final ObjectMapper objectMapper;

    public PageDraftHistoryServiceImpl(PageDraftHistoryMapper historyMapper,
                                        EditorLockService lockService,
                                        @Lazy PageDraftService draftService,
                                        ObjectMapper objectMapper) {
        this.historyMapper = historyMapper;
        this.lockService = lockService;
        this.draftService = draftService;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordRevision(Long pageId, Long draftId, String schemaJsonStr, String schemaHash, String remark, String createdBy) {
        if (pageId == null || schemaJsonStr == null || schemaJsonStr.trim().isEmpty()) {
            return;
        }

        // 1. 查询当前页面最大的 revision_no
        LambdaQueryWrapper<PageDraftHistoryEntity> countWrapper = new LambdaQueryWrapper<>();
        countWrapper.eq(PageDraftHistoryEntity::getPageId, pageId)
                    .orderByDesc(PageDraftHistoryEntity::getRevisionNo);
        PageDraftHistoryEntity latestHist = historyMapper.selectOne(countWrapper);
        int nextRev = (latestHist != null && latestHist.getRevisionNo() != null) ? latestHist.getRevisionNo() + 1 : 1;

        // 2. 插入新的草稿修订快照
        PageDraftHistoryEntity entity = new PageDraftHistoryEntity();
        entity.setPageId(pageId);
        entity.setDraftId(draftId != null ? draftId : 0L);
        entity.setRevisionNo(nextRev);
        entity.setSchemaJson(schemaJsonStr);
        entity.setSchemaHash(schemaHash != null ? schemaHash : "");
        entity.setEditorSessionRemark(remark != null ? remark : "草稿保存");
        entity.setCreatedBy(createdBy != null ? createdBy : "system");
        entity.setCreatedAt(LocalDateTime.now());
        historyMapper.insert(entity);

        // 3. 超额修剪：仅查询 id 与 revision_no 字段，保持单个页面最多保留 MAX_REVISIONS_PER_PAGE (20) 条最新历史
        LambdaQueryWrapper<PageDraftHistoryEntity> allHistWrapper = new LambdaQueryWrapper<>();
        allHistWrapper.select(PageDraftHistoryEntity::getId, PageDraftHistoryEntity::getRevisionNo)
                      .eq(PageDraftHistoryEntity::getPageId, pageId)
                      .orderByDesc(PageDraftHistoryEntity::getRevisionNo);
        List<PageDraftHistoryEntity> allList = historyMapper.selectList(allHistWrapper);
        if (allList.size() > MAX_REVISIONS_PER_PAGE) {
            List<Long> deleteIds = allList.subList(MAX_REVISIONS_PER_PAGE, allList.size())
                    .stream()
                    .map(PageDraftHistoryEntity::getId)
                    .collect(Collectors.toList());
            historyMapper.deleteBatchIds(deleteIds);
            log.info("Pruned {} old draft revisions for pageId: {}", deleteIds.size(), pageId);
        }
    }

    @Override
    public PageResult<PageDraftHistoryVO> getRevisionsPage(Long pageId, int pageNo, int pageSize) {
        Page<PageDraftHistoryEntity> page = new Page<>(pageNo, pageSize);
        LambdaQueryWrapper<PageDraftHistoryEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(PageDraftHistoryEntity::getId,
                       PageDraftHistoryEntity::getPageId,
                       PageDraftHistoryEntity::getDraftId,
                       PageDraftHistoryEntity::getRevisionNo,
                       PageDraftHistoryEntity::getSchemaHash,
                       PageDraftHistoryEntity::getEditorSessionRemark,
                       PageDraftHistoryEntity::getCreatedBy,
                       PageDraftHistoryEntity::getCreatedAt)
               .eq(PageDraftHistoryEntity::getPageId, pageId)
               .orderByDesc(PageDraftHistoryEntity::getRevisionNo);

        Page<PageDraftHistoryEntity> result = historyMapper.selectPage(page, wrapper);

        List<PageDraftHistoryVO> voList = result.getRecords().stream()
                .map(entity -> convertToVO(entity, false))
                .collect(Collectors.toList());

        return PageResult.build(voList, result.getTotal(), (int) result.getSize(), (int) result.getCurrent());
    }

    @Override
    public PageDraftHistoryVO getRevisionDetail(Long pageId, Long revisionId) {
        PageDraftHistoryEntity entity = historyMapper.selectById(revisionId);
        if (entity == null || !entity.getPageId().equals(pageId)) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "指定的草稿修订记录不存在");
        }
        return convertToVO(entity, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @AuditLog(action = "RESTORE_DRAFT_REVISION", resourceType = "PAGE_DRAFT_REVISION", resourceIdExpression = "#revisionId")
    public PageDraftVO restoreRevision(Long pageId, Long revisionId, Integer currentVersion, String lockToken) {
        // 1. 显式校验活动页面门禁 (requireActivePage)
        if (draftService instanceof PageDraftServiceImpl) {
            ((PageDraftServiceImpl) draftService).requireActivePage(pageId);
        }

        // 2. 校验编辑锁
        String currentOperator = SecurityUtils.getCurrentUsername();
        lockService.validateLockForEdit(pageId, lockToken, currentOperator);

        // 3. 查询修订详情
        PageDraftHistoryEntity entity = historyMapper.selectById(revisionId);
        if (entity == null || !entity.getPageId().equals(pageId)) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "指定的草稿修订记录不存在");
        }

        // 4. 解析 Schema 模型
        PageSchemaModel schemaModel;
        try {
            schemaModel = objectMapper.readValue(entity.getSchemaJson(), PageSchemaModel.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "草稿历史 Schema 损坏，无法恢复: " + e.getMessage());
        }

        // 5. 覆盖写入主草稿 (自增乐观锁 version，并记录恢复说明)
        String remark = "恢复至修订版本 #" + entity.getRevisionNo();
        return draftService.saveDraft(pageId, schemaModel, remark, currentVersion, lockToken, currentOperator);
    }

    private PageDraftHistoryVO convertToVO(PageDraftHistoryEntity entity, boolean includeSchema) {
        PageDraftHistoryVO vo = new PageDraftHistoryVO();
        vo.setId(entity.getId());
        vo.setPageId(entity.getPageId());
        vo.setDraftId(entity.getDraftId());
        vo.setRevisionNo(entity.getRevisionNo());
        vo.setSchemaHash(entity.getSchemaHash());
        vo.setEditorSessionRemark(entity.getEditorSessionRemark());
        vo.setCreatedBy(entity.getCreatedBy());
        vo.setCreatedAt(entity.getCreatedAt());

        if (includeSchema && entity.getSchemaJson() != null) {
            try {
                PageSchemaModel schema = objectMapper.readValue(entity.getSchemaJson(), PageSchemaModel.class);
                vo.setSchemaJson(schema);
            } catch (Exception e) {
                log.warn("Failed to parse schemaJson for history id: {}", entity.getId(), e);
            }
        }
        return vo;
    }
}
