package com.company.officialwebsite.modules.lead.controller;

import com.company.officialwebsite.common.response.ApiResponse;
import com.company.officialwebsite.modules.lead.service.CooperationDirectionTagService;
import com.company.officialwebsite.modules.lead.vo.PortalCooperationDirectionTagVO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * PortalCooperationDirectionTagController：提供前台公开合作方向标签列表读取接口。
 */
@RestController
@RequestMapping("/portal/api/cooperation-direction-tags")
public class PortalCooperationDirectionTagController {

    private static final Logger log = LoggerFactory.getLogger(PortalCooperationDirectionTagController.class);

    private final CooperationDirectionTagService cooperationDirectionTagService;

    public PortalCooperationDirectionTagController(CooperationDirectionTagService cooperationDirectionTagService) {
        this.cooperationDirectionTagService = cooperationDirectionTagService;
    }

    @GetMapping
    public ApiResponse<List<PortalCooperationDirectionTagVO>> getCooperationDirectionTags() {
        log.info("get portal cooperation direction tags request");
        return ApiResponse.success(cooperationDirectionTagService.getPortalCooperationDirectionTagList());
    }
}
