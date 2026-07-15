package com.company.officialwebsite.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DetailOfflineDTO：下线详情请求 DTO。
 */
public class DetailOfflineDTO {

    @NotNull(message = "乐观锁版本号不能为空")
    private Integer version;

    @NotBlank(message = "下线原因不能为空")
    @Size(max = 255, message = "下线原因最长为255字符")
    private String reason;

    public DetailOfflineDTO() {
    }

    public DetailOfflineDTO(Integer version, String reason) {
        this.version = version;
        this.reason = reason;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
