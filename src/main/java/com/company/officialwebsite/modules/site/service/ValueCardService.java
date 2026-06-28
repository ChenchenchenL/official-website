package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.site.dto.ValueCardBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.ValueCardCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.ValueCardUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminValueCardVO;
import com.company.officialwebsite.modules.site.vo.PortalValueCardVO;
import java.util.List;

/**
 * ValueCardService：封装核心价值观卡片的后台维护与前台读取能力。
 */
public interface ValueCardService {

    PageResult<AdminValueCardVO> getAdminValueCardList(int pageNo, int pageSize);

    void createValueCard(ValueCardCreateRequestDTO requestDTO);

    void updateValueCard(Long id, ValueCardUpdateRequestDTO requestDTO);

    void deleteValueCard(Long id, Integer version);

    void reorderValueCards(ValueCardBatchSortRequestDTO requestDTO);

    List<PortalValueCardVO> getPortalValueCards();
}
