package com.company.officialwebsite.modules.business.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessPageBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessPageUpdateRequestDTO;
import com.company.officialwebsite.modules.business.vo.AdminBusinessPageVO;

public interface BusinessPageService {

    PageResult<AdminBusinessPageVO> getAdminBusinessPageList(int pageNo, int pageSize);

    void createBusinessPage(BusinessPageCreateRequestDTO requestDTO);

    void updateBusinessPage(Long id, BusinessPageUpdateRequestDTO requestDTO);

    void deleteBusinessPage(Long id, Integer version);

    void reorderBusinessPages(BusinessPageBatchSortRequestDTO requestDTO);
}
