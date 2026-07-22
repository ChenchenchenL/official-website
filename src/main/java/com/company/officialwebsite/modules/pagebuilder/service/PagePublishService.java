package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.common.response.PageResult;
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

    /**
     * 分页查询指定页面的历史发布版本摘要列表（不包含大字段 schemaJson）。
     *
     * @param pageId   页面定义 ID
     * @param pageNo   页码，从 1 开始
     * @param pageSize 每页条数
     * @return 分页版本摘要 VO 列表
     */
    PageResult<PageVersionVO> listVersions(Long pageId, int pageNo, int pageSize);

    /**
     * 查询指定页面单个历史版本的全量配置详情（包含完整 schemaJson）。
     *
     * @param pageId    页面定义 ID
     * @param versionId 历史版本记录 ID
     * @return 包含完整 Schema 的版本 VO
     */
    PageVersionVO getVersionDetail(Long pageId, Long versionId);
}
