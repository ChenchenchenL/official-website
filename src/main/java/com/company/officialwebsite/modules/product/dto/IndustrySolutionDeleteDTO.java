package com.company.officialwebsite.modules.product.dto;

import jakarta.validation.constraints.NotNull;

/**
 * IndustrySolutionDeleteDTO：删除行业解决方案请求参数。
 */
public class IndustrySolutionDeleteDTO {

    @NotNull(message = "乐观锁版本号不能为空")
    private Integer version;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
