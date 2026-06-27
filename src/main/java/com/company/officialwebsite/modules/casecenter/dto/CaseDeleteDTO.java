package com.company.officialwebsite.modules.casecenter.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * CaseDeleteDTO：删除标杆案例请求参数。
 */
public class CaseDeleteDTO {

    @NotNull(message = "乐观锁版本号不能为空")
    @PositiveOrZero(message = "乐观锁版本号不能为负数")
    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
