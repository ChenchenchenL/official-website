package com.company.officialwebsite.modules.business.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateBatchSortRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateCreateBusinessRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateCreateRequestDTO;
import com.company.officialwebsite.modules.business.dto.BusinessTemplateUpdateRequestDTO;
import com.company.officialwebsite.modules.business.vo.AdminBusinessTemplateVO;

public interface BusinessTemplateService {

    PageResult<AdminBusinessTemplateVO> getAdminBusinessTemplateList(int pageNo, int pageSize);

    void createBusinessTemplate(BusinessTemplateCreateRequestDTO requestDTO);

    void updateBusinessTemplate(Long id, BusinessTemplateUpdateRequestDTO requestDTO);

    void deleteBusinessTemplate(Long id, Integer version);

    void copyBusinessTemplate(Long id, Integer version);

    void createBusinessFromTemplate(Long id, BusinessTemplateCreateBusinessRequestDTO requestDTO);

    void reorderBusinessTemplates(BusinessTemplateBatchSortRequestDTO requestDTO);
}
