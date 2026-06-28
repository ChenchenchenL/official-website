package com.company.officialwebsite.modules.lead.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.lead.dto.ContactInfoUpdateRequestDTO;
import com.company.officialwebsite.modules.lead.service.ContactInfoService;
import com.company.officialwebsite.modules.lead.vo.AdminContactInfoVO;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AdminContactInfoController：提供后台基础联系方式管理接口。
 */
@Validated
@RestController
@RequestMapping("/admin/api/contact-info")
public class AdminContactInfoController {

    private static final Logger log = LoggerFactory.getLogger(AdminContactInfoController.class);

    private final ContactInfoService contactInfoService;

    public AdminContactInfoController(ContactInfoService contactInfoService) {
        this.contactInfoService = contactInfoService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<AdminContactInfoVO> getContactInfo() {
        log.info("get contact info request");
        return ApiResponse.success(contactInfoService.getAdminContactInfo());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ApiResponse<Void> updateContactInfo(@Valid @RequestBody ContactInfoUpdateRequestDTO requestDTO) {
        log.info("update contact info request version={}", requestDTO.getVersion());
        contactInfoService.updateContactInfo(requestDTO);
        return ApiResponse.success();
    }
}
