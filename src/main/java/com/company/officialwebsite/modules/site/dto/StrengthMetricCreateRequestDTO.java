package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * StrengthMetricCreateRequestDTO：承载后台新增企业实力核心指标的请求参数。
 * iconId 为选填，允许创建无图标的指标。
 */
public class StrengthMetricCreateRequestDTO {

    /** 指标图标媒体文件ID，选填。关联 media_file.id，Service 层负责校验有效性。 */
    private Long iconId;

    @NotBlank(message = "核心数值不能为空")
    @Size(max = 64, message = "核心数值长度不能超过64个字符")
    private String metricValue;

    @NotBlank(message = "业务标签不能为空")
    @Size(max = 128, message = "业务标签长度不能超过128个字符")
    private String label;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

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
}
