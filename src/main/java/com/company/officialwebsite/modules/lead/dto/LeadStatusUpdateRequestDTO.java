package com.company.officialwebsite.modules.lead.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * LeadStatusUpdateRequestDTO：承载后台线索状态流转的请求参数。
 */
public class LeadStatusUpdateRequestDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotNull(message = "状态值不能为空")
    @PositiveOrZero(message = "状态值不能为负数")
    @Min(value = 0, message = "状态值不能小于 0")
    @Max(value = 3, message = "状态值不能大于 3")
    private Integer status;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
