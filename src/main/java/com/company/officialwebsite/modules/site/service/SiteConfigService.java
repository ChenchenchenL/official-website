package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.modules.site.dto.SiteConfigUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminSiteConfigVO;
import com.company.officialwebsite.modules.site.vo.PortalSiteConfigVO;

/**
 * SiteConfigService：封装站点基础配置的后台维护和前台读取能力。
 */
public interface SiteConfigService {

    /**
     * 获取后台编辑页所需的站点配置详情。
     */
    AdminSiteConfigVO getAdminConfig();

    /**
     * 更新站点基础配置并返回最新结果。
     */
    AdminSiteConfigVO updateConfig(SiteConfigUpdateRequestDTO requestDTO);

    /**
     * 获取前台公开可见的站点配置。
     */
    PortalSiteConfigVO getPortalConfig();
}
