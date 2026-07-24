package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.vo.PublishReviewVO;
import com.company.officialwebsite.modules.pagebuilder.vo.SchemaDiffItemVO;

import java.util.List;

/**
 * PageDiffService: 页面 Schema 版本差异计算与发布预审服务接口。
 */
public interface PageDiffService {

    /**
     * 计算指定页面的草稿与在线 ACTIVE 快照 (或指定历史版本) 的深层 Schema 变更明细列表。
     */
    List<SchemaDiffItemVO> comparePageSchema(Long pageId, Long compareVersion);

    /**
     * 生成当前草稿发布前的综合审阅概览视图 (包含发布预校验、绑定源摘要、版本对照与 Diff 列表)。
     */
    PublishReviewVO generatePublishReview(Long pageId);
}
