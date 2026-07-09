package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PromiseTagCreateRequestDTO：承载后台新增承诺标签的请求参数。
 */
public class PromiseTagCreateRequestDTO {

    @NotBlank(message = "标签文本不能为空")
    @Size(max = 32, message = "标签文本长度不能超过32个字符")
    private String tagText;

    @Size(max = 255, message = "标签描述长度不能超过255个字符")
    private String description;

    private Boolean visible;

    public String getTagText() {
        return tagText;
    }

    public void setTagText(String tagText) {
        this.tagText = tagText;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
