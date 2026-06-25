package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * StrengthMetricUpdateRequestDTO：承载后台编辑企业实力核心指标的请求参数。
 * 必须携带 version 以执行乐观锁并发保护，防止并发覆盖。
 */
public class StrengthMetricUpdateRequestDTO {

    /** 乐观锁版本号，防止并发覆盖，必填。 */
    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    /** 指标图标媒体文件ID，选填。传 null 表示清除图标并解绑旧媒体资源。 */
    private Long iconId;

    @NotBlank(message = "核心数值不能为空")
    @Size(max = 64, message = "核心数值长度不能超过64个字符")
    private String metricValue;

    @NotBlank(message = "业务标签不能为空")
    @Size(max = 128, message = "业务标签长度不能超过128个字符")
    private String label;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

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
