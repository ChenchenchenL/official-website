package com.company.officialwebsite.modules.business.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessBlockBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessBlockCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessBlockUpdateRequestDTO;
import com.company.officialwebsite.modules.business.vo.AdminBusinessBlockVO;

public interface BusinessBlockService {

    PageResult<AdminBusinessBlockVO> getAdminBusinessBlockList(int pageNo, int pageSize);

    void createBusinessBlock(BusinessBlockCreateRequestDTO requestDTO);

    void updateBusinessBlock(Long id, BusinessBlockUpdateRequestDTO requestDTO);

    void deleteBusinessBlock(Long id, Integer version);

    void reorderBusinessBlocks(BusinessBlockBatchSortRequestDTO requestDTO);
}
