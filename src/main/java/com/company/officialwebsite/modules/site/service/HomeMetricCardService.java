package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.modules.site.dto.HomeMetricCardCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardOrderRequestDTO;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardUpdateRequestDTO;
import com.company.officialwebsite.modules.site.dto.HomeMetricCardVisibilityUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminHomeMetricCardVO;
import com.company.officialwebsite.modules.site.vo.PortalHomeMetricCardVO;
import java.util.List;

/**
 * HomeMetricCardService：封装首页核心数据指标卡片后台维护和前台读取能力。
 */
public interface HomeMetricCardService {

    /**
     * 获取后台可编辑的首页核心数据指标卡片列表。
     */
    List<AdminHomeMetricCardVO> getAdminMetricCards();

    /**
     * 新增首页核心数据指标卡片并返回最新列表。
     */
    List<AdminHomeMetricCardVO> createMetricCard(HomeMetricCardCreateRequestDTO requestDTO);

    /**
     * 更新首页核心数据指标卡片内容并返回最新列表。
     */
    List<AdminHomeMetricCardVO> updateMetricCard(Long metricId, HomeMetricCardUpdateRequestDTO requestDTO);

    /**
     * 更新首页核心数据指标卡片显示状态并返回最新列表。
     */
    List<AdminHomeMetricCardVO> updateVisibility(Long metricId, HomeMetricCardVisibilityUpdateRequestDTO requestDTO);

    /**
     * 删除首页核心数据指标卡片并返回最新列表。
     */
    List<AdminHomeMetricCardVO> deleteMetricCard(Long metricId);

    /**
     * 重排首页核心数据指标卡片并返回最新列表。
     */
    List<AdminHomeMetricCardVO> reorderMetricCards(HomeMetricCardOrderRequestDTO requestDTO);

    /**
     * 获取前台公开可见的首页核心数据指标卡片列表。
     */
    List<PortalHomeMetricCardVO> getPortalMetricCards();
}
