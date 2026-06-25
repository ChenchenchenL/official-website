package com.company.officialwebsite.modules.site.service;

import com.company.officialwebsite.modules.site.dto.StrengthMetricBatchSortRequestDTO;
import com.company.officialwebsite.modules.site.dto.StrengthMetricCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.StrengthMetricUpdateRequestDTO;
import com.company.officialwebsite.modules.site.vo.AdminStrengthMetricVO;
import com.company.officialwebsite.modules.site.vo.PortalStrengthMetricVO;
import java.util.List;

/**
 * StrengthMetricService：封装企业实力核心指标大盘的后台维护和前台读取能力。
 * 写操作由 StrengthMetricServiceImpl 负责事务、审计、缓存失效和媒体绑定。
 */
public interface StrengthMetricService {

    /**
     * 获取后台全量活跃指标列表（按 sort_order 升序）。
     */
    List<AdminStrengthMetricVO> getAdminMetrics();

    /**
     * 新增企业实力核心指标，返回最新全量列表。
     */
    List<AdminStrengthMetricVO> createMetric(StrengthMetricCreateRequestDTO requestDTO);

    /**
     * 编辑企业实力核心指标，含乐观锁版本校验，返回最新全量列表。
     */
    List<AdminStrengthMetricVO> updateMetric(Long metricId, StrengthMetricUpdateRequestDTO requestDTO);

    /**
     * 逻辑删除企业实力核心指标，解绑图标媒体资源，返回最新全量列表。
     */
    List<AdminStrengthMetricVO> deleteMetric(Long metricId, Integer version);

    /**
     * 批量拖拽排序，传入有序指标 ID 列表，后端自动计算 sort_order，返回最新全量列表。
     * 传入列表必须完整覆盖全部活跃指标，不允许遗漏或重复。
     */
    List<AdminStrengthMetricVO> reorderMetrics(StrengthMetricBatchSortRequestDTO requestDTO);

    /**
     * 获取前台公开可见指标列表（仅 visible=1 且未删除），支持 Redis 缓存。
     */
    List<PortalStrengthMetricVO> getPortalMetrics();
}
