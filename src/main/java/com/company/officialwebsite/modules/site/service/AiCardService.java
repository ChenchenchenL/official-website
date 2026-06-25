package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.AiCardCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.AiCardSortItemDTO;
import com.company.officialwebsite.modules.site.dto.AiCardUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminAiCardVO;
import com.company.officialwebsite.modules.site.vo.PortalAiCardVO;
import java.util.List;

/**
 * AiCardService：AI 战略卡片管理的业务接口。
 */
public interface AiCardService {

    /**
     * 分页查询后台 AI 卡片列表。
     */
    PageResult<AdminAiCardVO> getAdminCards(int pageNo, int pageSize);

    /**
     * 新增 AI 卡片，返回新增记录的 ID。
     */
    Long createCard(AiCardCreateRequestDTO requestDTO);

    /**
     * 编辑 AI 卡片，执行乐观锁并发保护。
     */
    void updateCard(Long id, AiCardUpdateRequestDTO requestDTO);

    /**
     * 逻辑删除 AI 卡片。
     */
    void deleteCard(Long id, Integer version);

    /**
     * 批量排序。
     */
    void batchSortCards(List<AiCardSortItemDTO> requestDTO);

    /**
     * 获取前台展示的可见 AI 战略卡片列表。
     */
    List<PortalAiCardVO> getPortalCards();
}
