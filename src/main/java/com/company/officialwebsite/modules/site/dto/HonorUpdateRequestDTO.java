package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * HonorUpdateRequestDTO：承载后台编辑荣誉标签的请求参数。
 */
public class HonorUpdateRequestDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotBlank(message = "荣誉名称不能为空")
    @Size(max = 120, message = "荣誉名称长度不能超过120个字符")
    private String name;

    @NotNull(message = "荣誉图标不能为空")
    private Long iconId;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getIconId() {
        return iconId;
    }

    public void setIconId(Long iconId) {
        this.iconId = iconId;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
