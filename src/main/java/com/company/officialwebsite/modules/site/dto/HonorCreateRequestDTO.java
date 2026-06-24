package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * HonorCreateRequestDTO：承载后台新增荣誉标签的请求参数。
 */
public class HonorCreateRequestDTO {

    @NotBlank(message = "荣誉名称不能为空")
    @Size(max = 120, message = "荣誉名称长度不能超过120个字符")
    private String name;

    @NotNull(message = "荣誉图标不能为空")
    private Long iconId;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

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
