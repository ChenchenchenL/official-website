package com.company.officialwebsite.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * LockForceReleaseDTO：强制释放锁请求体。
 */
public class LockForceReleaseDTO {

    @NotBlank(message = "强制解锁原因不能为空")
    @Size(max = 255, message = "强制解锁原因最长为255字符")
    private String reason;

    public LockForceReleaseDTO() {
    }

    public LockForceReleaseDTO(String reason) {
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
