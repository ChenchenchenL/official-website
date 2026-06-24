package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * HomeMetricCardUpdateRequestDTO：承载后台编辑首页核心数据指标卡片的请求参数。
 */
public class HomeMetricCardUpdateRequestDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotBlank(message = "数值不能为空")
    @Size(max = 32, message = "数值长度不能超过32个字符")
    private String value;

    @Size(max = 32, message = "单位长度不能超过32个字符")
    private String unit;

    @NotBlank(message = "描述文案不能为空")
    @Size(max = 120, message = "描述文案长度不能超过120个字符")
    private String description;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

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
