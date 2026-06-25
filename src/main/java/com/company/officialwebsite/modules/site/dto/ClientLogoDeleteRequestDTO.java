package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * ClientLogoDeleteRequestDTO：承载后台删除客户 Logo 时的乐观锁参数。
 */
public class ClientLogoDeleteRequestDTO {

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
