package com.company.officialwebsite.modules.lead.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * CooperationDirectionTagUpdateRequestDTO：承载后台编辑合作方向标签的请求参数。
 */
public class CooperationDirectionTagUpdateRequestDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotBlank(message = "标签文本不能为空")
    @Size(max = 32, message = "标签文本长度不能超过32个字符")
    private String tagText;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTagText() {
        return tagText;
    }

    public void setTagText(String tagText) {
        this.tagText = tagText;
    }
}
