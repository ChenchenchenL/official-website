package com.company.officialwebsite.modules.lead.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * CooperationDirectionTagCreateRequestDTO：承载后台新增合作方向标签的请求参数。
 */
public class CooperationDirectionTagCreateRequestDTO {

    @NotBlank(message = "标签文本不能为空")
    @Size(max = 32, message = "标签文本长度不能超过32个字符")
    private String tagText;

    public String getTagText() {
        return tagText;
    }

    public void setTagText(String tagText) {
        this.tagText = tagText;
    }
}
