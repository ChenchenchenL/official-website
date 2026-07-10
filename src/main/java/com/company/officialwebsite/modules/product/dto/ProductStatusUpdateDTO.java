package com.company.officialwebsite.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ProductStatusUpdateDTO {

    @NotBlank(message = "状态不能为空")
    private String status;

    @NotNull(message = "乐观锁版本号不能为空")
    private Integer version;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
