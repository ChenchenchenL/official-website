package com.company.officialwebsite.modules.pagebuilder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftHistoryEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * PageDraftHistoryMapper: 页面草稿历史修订持久化 Mapper 接口。
 */
@Mapper
public interface PageDraftHistoryMapper extends BaseMapper<PageDraftHistoryEntity> {
}
