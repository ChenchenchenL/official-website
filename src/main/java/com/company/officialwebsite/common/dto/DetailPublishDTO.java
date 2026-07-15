package com.company.officialwebsite.common.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DetailPublishDTO：发布详情请求 DTO。
 */
public class DetailPublishDTO {

    @NotBlank(message = "发布变更说明不能为空")
    @Size(max = 255, message = "变更说明最长为255字符")
    private String changeSummary;

    @NotNull(message = "乐观锁版本号不能为空")
    private Integer version;

    public DetailPublishDTO() {
    }

    public DetailPublishDTO(String changeSummary, Integer version) {
        this.changeSummary = changeSummary;
        this.version = version;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
