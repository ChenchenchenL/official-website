package com.company.officialwebsite.modules.lead.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.lead.service.ContactInfoService;
import com.company.officialwebsite.modules.lead.vo.PortalContactInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalContactInfoController：提供前台公开基础联系方式读取接口。
 */
@RestController
@RequestMapping("/portal/api/contact-info")
public class PortalContactInfoController {

    private static final Logger log = LoggerFactory.getLogger(PortalContactInfoController.class);

    private final ContactInfoService contactInfoService;

    public PortalContactInfoController(ContactInfoService contactInfoService) {
        this.contactInfoService = contactInfoService;
    }

    @GetMapping
    public ApiResponse<PortalContactInfoVO> getContactInfo() {
        log.info("get portal contact info request");
        return ApiResponse.success(contactInfoService.getPortalContactInfo());
    }
}
