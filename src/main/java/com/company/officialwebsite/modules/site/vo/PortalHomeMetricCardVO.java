package com.company.officialwebsite.modules.site.vo;

/**
 * PortalHomeMetricCardVO：前台公开首页核心数据指标卡片返回对象。
 */
public class PortalHomeMetricCardVO {

    private String value;
    private String unit;
    private String description;

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
