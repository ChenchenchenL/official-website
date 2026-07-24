package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftHistoryVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;

/**
 * PageDraftHistoryService: 页面草稿历史修订服务接口。
 */
public interface PageDraftHistoryService {

    /**
     * 记录一条新的草稿历史快照，并物理裁剪超过 20 条的最老历史。
     */
    void recordRevision(Long pageId, Long draftId, String schemaJsonStr, String schemaHash, String remark, String createdBy);

    /**
     * 分页查询页面草稿历史修订摘要列表。
     */
    PageResult<PageDraftHistoryVO> getRevisionsPage(Long pageId, int pageNo, int pageSize);

    /**
     * 查询单条草稿历史修订完整详情 (包含 schemaJson)。
     */
    PageDraftHistoryVO getRevisionDetail(Long pageId, Long revisionId);

    /**
     * 恢复指定草稿历史修订为当前主草稿 (需编辑锁凭证与并发乐观锁控制)。
     */
    PageDraftVO restoreRevision(Long pageId, Long revisionId, Integer currentVersion, String lockToken);
}
