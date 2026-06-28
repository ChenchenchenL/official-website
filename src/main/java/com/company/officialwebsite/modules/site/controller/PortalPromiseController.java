package com.company.officialwebsite.modules.site.controller;

import com.company.officialwebsite.application.portal.PromisePortalApplicationService;
import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.site.vo.PortalPromiseModuleVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalPromiseController：提供前台公开"我们的承诺"模块聚合接口。
 */
@RestController
@RequestMapping("/portal/api/our-promises")
public class PortalPromiseController {

    private static final Logger log = LoggerFactory.getLogger(PortalPromiseController.class);

    private final PromisePortalApplicationService promisePortalApplicationService;

    public PortalPromiseController(PromisePortalApplicationService promisePortalApplicationService) {
        this.promisePortalApplicationService = promisePortalApplicationService;
    }

    @GetMapping
    public ApiResponse<PortalPromiseModuleVO> getPromiseModule() {
        log.info("get portal promise module request");
        return ApiResponse.success(promisePortalApplicationService.getPortalPromiseModule());
    }
}
