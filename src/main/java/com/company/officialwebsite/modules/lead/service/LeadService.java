package com.company.officialwebsite.modules.lead.service;

import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.modules.lead.dto.LeadCreateRequestDTO;
import com.company.officialwebsite.modules.lead.dto.LeadExportRequestDTO;
import com.company.officialwebsite.modules.lead.dto.LeadQueryRequestDTO;
import com.company.officialwebsite.modules.lead.dto.LeadStatusUpdateRequestDTO;
import com.company.officialwebsite.modules.lead.vo.AdminLeadDetailVO;
import com.company.officialwebsite.modules.lead.vo.AdminLeadVO;
import jakarta.servlet.http.HttpServletResponse;

/**
 * LeadService：封装前台线索提交与后台线索看板的核心业务能力。
 */
public interface LeadService {

    void createLead(LeadCreateRequestDTO requestDTO, String clientIp, String userAgent);

    PageResult<AdminLeadVO> getAdminLeadPage(LeadQueryRequestDTO requestDTO);

    AdminLeadDetailVO getAdminLeadDetail(Long id);

    void updateLeadStatus(Long id, LeadStatusUpdateRequestDTO requestDTO, Long operatorId);

    void exportLeads(LeadExportRequestDTO requestDTO, HttpServletResponse response, Long operatorId);
}
