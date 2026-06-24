package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.modules.site.dto.HomeBannerUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminHomeBannerVO;
import com.company.officialwebsite.modules.site.vo.PortalHomeBannerVO;

/**
 * HomeBannerService：封装首页首屏主视觉配置的后台维护和前台读取能力。
 */
public interface HomeBannerService {

    /**
     * 获取后台编辑页所需的首页首屏配置详情。
     */
    AdminHomeBannerVO getAdminBanner();

    /**
     * 更新首页首屏主视觉配置并返回最新结果。
     */
    AdminHomeBannerVO updateBanner(HomeBannerUpdateRequestDTO requestDTO);

    /**
     * 获取前台公开可见的首页首屏主视觉配置。
     */
    PortalHomeBannerVO getPortalBanner();
}
