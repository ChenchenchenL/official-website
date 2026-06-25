package com.company.officialwebsite.modules.site.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * StrengthMetricEntity：记录企业实力核心指标大盘中的单条指标，
 * 对应表 cms_strength_metric。每条记录表示一个独立的量化指标卡片，
 * 如"50+ 服务客户"、"100% 项目交付"等。
 */
@TableName("cms_strength_metric")
public class StrengthMetricEntity extends BaseEntity {

    /** 指标图标媒体文件ID，允许为 null（未配置图标时）。 */
    private Long iconId;

    /** 核心数值，如 50+、100%、5大领域，最长 64 字符。 */
    private String metricValue;

    /** 业务标签/描述文案，如 服务客户、项目交付，最长 128 字符。 */
    private String label;

    /** 前台可见状态，true 显示，false 隐藏。 */
    private Boolean visible;

    /** 排序值，越小越靠前，默认 99。 */
    private Integer sortOrder;

    public Long getIconId() {
        return iconId;
    }

    public void setIconId(Long iconId) {
        this.iconId = iconId;
    }

    public String getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(String metricValue) {
        this.metricValue = metricValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
