package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.dto.PagePublishDTO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageVersionVO;

import java.util.List;

/**
 * PagePublishService: 页面版本快照发布与回滚管理服务接口。
 */
public interface PagePublishService {

    /**
     * 将指定页面的草稿发布上线，生成新的历史版本、快照，并提取依赖、清空缓存。
     *
     * @param pageId 页面定义 ID
     * @param dto    发布请求参数
     * @return 生成的历史版本 VO
     */
    PageVersionVO publishPage(Long pageId, PagePublishDTO dto);

    /**
     * 将指定页面回滚到特定的历史发布版本，生成新的回滚版本与快照，同步更新草稿并刷新缓存。
     *
     * @param pageId    页面定义 ID
     * @param versionId 历史页面版本 ID
     * @return 新生成的历史回滚版本 VO
     */
    PageVersionVO rollbackPage(Long pageId, Long versionId);

    /**
     * 获取指定页面的所有历史发布/保存版本列表（按版本序号倒序）。
     *
     * @param pageId 页面定义 ID
     * @return 页面版本 VO 列表
     */
    List<PageVersionVO> listVersions(Long pageId);
}
