package com.company.officialwebsite.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DetailRollbackDTO：回滚详情请求 DTO。
 */
public class DetailRollbackDTO {

    @NotNull(message = "乐观锁版本号不能为空")
    private Integer version;

    @Size(max = 255, message = "回滚变更说明最长为255字符")
    private String changeSummary;

    public DetailRollbackDTO() {
    }

    public DetailRollbackDTO(Integer version, String changeSummary) {
        this.version = version;
        this.changeSummary = changeSummary;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }
}
