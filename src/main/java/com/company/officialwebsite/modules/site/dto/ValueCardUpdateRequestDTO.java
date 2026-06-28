package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ValueCardUpdateRequestDTO：承载后台编辑核心价值观卡片的请求参数。
 */
public class ValueCardUpdateRequestDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotNull(message = "图标媒体 ID 不能为空")
    private Long iconMediaId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 32, message = "标题长度不能超过32个字符")
    private String title;

    @NotBlank(message = "副标语不能为空")
    @Size(max = 128, message = "副标语长度不能超过128个字符")
    private String subtitle;

    @NotBlank(message = "描述不能为空")
    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getIconMediaId() {
        return iconMediaId;
    }

    public void setIconMediaId(Long iconMediaId) {
        this.iconMediaId = iconMediaId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
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
