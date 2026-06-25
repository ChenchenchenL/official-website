package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * AiCardCreateRequestDTO：承载后台新增 AI 战略卡片的请求参数。
 */
public class AiCardCreateRequestDTO {

    @NotBlank(message = "中文主标题不能为空")
    @Size(max = 128, message = "中文主标题长度不能超过128个字符")
    private String name;

    @Size(max = 128, message = "英文副标长度不能超过128个字符")
    private String englishName;

    private Long iconId;

    @NotBlank(message = "一句话描述不能为空")
    @Size(max = 256, message = "一句话描述长度不能超过256个字符")
    private String description;

    @Size(max = 256, message = "跳转链接长度不能超过256个字符")
    private String jumpLink;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    private Integer sortOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public Long getIconId() {
        return iconId;
    }

    public void setIconId(Long iconId) {
        this.iconId = iconId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJumpLink() {
        return jumpLink;
    }

    public void setJumpLink(String jumpLink) {
        this.jumpLink = jumpLink;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
