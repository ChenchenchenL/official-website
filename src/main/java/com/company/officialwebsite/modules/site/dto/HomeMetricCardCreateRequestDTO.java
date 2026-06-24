package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * HomeMetricCardCreateRequestDTO：承载后台新增首页核心数据指标卡片的请求参数。
 */
public class HomeMetricCardCreateRequestDTO {

    @NotBlank(message = "数值不能为空")
    @Size(max = 32, message = "数值长度不能超过32个字符")
    private String value;

    @Size(max = 32, message = "单位长度不能超过32个字符")
    private String unit;

    @NotBlank(message = "描述文案不能为空")
    @Size(max = 120, message = "描述文案长度不能超过120个字符")
    private String description;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

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

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
