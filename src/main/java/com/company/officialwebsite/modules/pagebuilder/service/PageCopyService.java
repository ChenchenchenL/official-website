package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.dto.PageCopyDTO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDefinitionVO;

import java.util.Map;

/**
 * PageCopyService: 页面复制、模板建页与共享区块影响诊断服务接口。
 */
public interface PageCopyService {

    /**
     * 从已有页面或预设模板复制创建新页面（自动重新分配组件 Section ID 并重置唯一路由与 Key）。
     */
    PageDefinitionVO copyPage(PageCopyDTO dto, String operator);

    /**
     * 诊断指定共享区块 (Shared Block) 被哪些活跃页面/草稿引用及影响范围。
     */
    Map<String, Object> diagnoseSharedBlockImpact(Long blockId);
}
