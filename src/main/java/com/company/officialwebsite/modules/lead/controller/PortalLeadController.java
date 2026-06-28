package com.company.officialwebsite.modules.lead.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.common.utils.ClientIpResolver;
import com.company.officialwebsite.modules.lead.dto.LeadCreateRequestDTO;
import com.company.officialwebsite.modules.lead.service.LeadService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalLeadController：提供前台"预约交流"匿名表单提交入口。
 */
@RestController
@RequestMapping("/portal/api/leads")
public class PortalLeadController {

    private static final Logger log = LoggerFactory.getLogger(PortalLeadController.class);

    private final LeadService leadService;

    public PortalLeadController(LeadService leadService) {
        this.leadService = leadService;
    }

    @PostMapping
    public ApiResponse<Void> createLead(
            @Valid @RequestBody LeadCreateRequestDTO requestDTO,
            HttpServletRequest request) {
        String clientIp = ClientIpResolver.resolve(request);
        String userAgent = request.getHeader("User-Agent");
        log.info("portal lead submit request name={} company={} ip={}", requestDTO.getName(), requestDTO.getCompany(), clientIp);
        leadService.createLead(requestDTO, clientIp, userAgent);
        return ApiResponse.success();
    }
}
