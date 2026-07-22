package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDependencyVO;

/**
 * PageDependencyQueryService：后台页面发布依赖的只读诊断服务。
 */
public interface PageDependencyQueryService {

    /**
     * 分页查询页面当前持久化的发布依赖。
     *
     * @param pageId 页面定义 ID
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页数量，最大 100
     * @return 页面依赖分页结果
     */
    PageResult<PageDependencyVO> getPublishedDependencies(Long pageId, int pageNo, int pageSize);
}
