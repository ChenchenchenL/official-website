package com.company.officialwebsite.modules.business.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessRegistryUpdateRequestDTO;
import com.company.officialwebsite.modules.business.vo.AdminBusinessRegistryVO;

public interface BusinessRegistryService {

    PageResult<AdminBusinessRegistryVO> getAdminBusinessRegistryList(int pageNo, int pageSize);

    void createBusinessRegistry(BusinessRegistryCreateRequestDTO requestDTO);

    void updateBusinessRegistry(Long id, BusinessRegistryUpdateRequestDTO requestDTO);

    void deleteBusinessRegistry(Long id, Integer version);

    void reorderBusinessRegistry(BusinessRegistryBatchSortRequestDTO requestDTO);
}
