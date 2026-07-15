package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.dto.PagePublishDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageRollbackDTO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageVersionVO;

import java.util.List;

/**
 * PagePublishService: 页面版本快照发布与回滚管理服务接口。
 */
public interface PagePublishService {

    /**
     * 将指定页面的草稿发布上线，生成新的历史版本、快照，并提取依赖、清空缓存。
     * 发布前校验草稿乐观锁版本，防止并发覆盖。
     */
    PageVersionVO publishPage(Long pageId, PagePublishDTO dto, String lockToken, String operator);

    /**
     * 将指定页面回滚到特定的历史发布版本，生成新的回滚版本与快照，同步更新草稿并刷新缓存。
     * 回滚前校验草稿乐观锁版本，防止并发覆盖。
     */
    PageVersionVO rollbackPage(Long pageId, PageRollbackDTO dto, String lockToken, String operator);

    /**
     * 获取指定页面的所有历史发布/保存版本列表（按版本序号倒序）。
     */
    List<PageVersionVO> listVersions(Long pageId);
}
