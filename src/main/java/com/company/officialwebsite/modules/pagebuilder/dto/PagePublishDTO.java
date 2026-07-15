package com.company.officialwebsite.modules.pagebuilder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PagePublishDTO: 页面发布请求对象。
 */
public class PagePublishDTO {

    @NotBlank(message = "页面发布变更描述不能为空")
    @Size(max = 255, message = "页面发布变更描述长度不能超过255字符")
    private String changeSummary;

    @NotNull(message = "草稿版本号不能为空")
    private Integer version;

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
