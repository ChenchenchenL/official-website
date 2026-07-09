package com.company.officialwebsite.modules.business.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessPageBlockBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageBlockCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageBlockUpdateRequestDTO;
import com.company.officialwebsite.modules.business.vo.AdminBusinessPageBlockVO;

public interface BusinessPageBlockService {

    PageResult<AdminBusinessPageBlockVO> getAdminBusinessPageBlockList(int pageNo, int pageSize);

    void createBusinessPageBlock(BusinessPageBlockCreateRequestDTO requestDTO);

    void updateBusinessPageBlock(Long id, BusinessPageBlockUpdateRequestDTO requestDTO);

    void deleteBusinessPageBlock(Long id, Integer version);

    void reorderBusinessPageBlocks(BusinessPageBlockBatchSortRequestDTO requestDTO);
}
