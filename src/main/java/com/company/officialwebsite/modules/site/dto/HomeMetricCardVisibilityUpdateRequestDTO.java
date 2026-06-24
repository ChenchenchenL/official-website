package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HomeMetricCardVisibilityUpdateRequestDTO：承载后台切换首页核心数据指标卡片显示状态的请求参数。
 */
public class HomeMetricCardVisibilityUpdateRequestDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
