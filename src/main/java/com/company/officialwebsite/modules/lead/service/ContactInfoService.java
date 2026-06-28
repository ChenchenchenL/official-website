package com.company.officialwebsite.modules.lead.service;

import com.company.officialwebsite.modules.lead.dto.ContactInfoUpdateRequestDTO;
import com.company.officialwebsite.modules.lead.vo.AdminContactInfoVO;
import com.company.officialwebsite.modules.lead.vo.PortalContactInfoVO;

/**
 * ContactInfoService：封装基础联系方式的后台维护与前台读取能力。
 */
public interface ContactInfoService {

    AdminContactInfoVO getAdminContactInfo();

    void updateContactInfo(ContactInfoUpdateRequestDTO requestDTO);

    PortalContactInfoVO getPortalContactInfo();
}
