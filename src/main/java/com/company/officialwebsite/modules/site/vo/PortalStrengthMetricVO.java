package com.company.officialwebsite.modules.site.vo;

/**
 * PortalStrengthMetricVO：前台公开企业实力核心指标返回对象。
 * 仅包含前台渲染所需的最少字段，禁止输出 version、deleted_marker 及系统审计字段。
 * iconUrl 在 iconId 为 null 时返回空字符串，前台需做空值容错。
 */
public class PortalStrengthMetricVO {

    private Long id;
    private String metricValue;
    private String label;

    /** 图标访问URL，未配置图标时为空字符串。 */
    private String iconUrl;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
