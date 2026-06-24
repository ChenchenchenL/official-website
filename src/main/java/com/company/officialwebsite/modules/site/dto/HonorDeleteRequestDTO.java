package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * HonorDeleteRequestDTO：承载后台删除荣誉标签时的乐观锁参数。
 */
public class HonorDeleteRequestDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
