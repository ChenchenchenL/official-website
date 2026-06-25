package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * CapabilityItemCreateDTO：承载后台新增具体底座子项的请求参数。
 */
public class CapabilityItemCreateDTO {

    @NotNull(message = "关联分类ID不能为空")
    private Long categoryId;

    @NotBlank(message = "子项名称不能为空")
    @Size(max = 128, message = "子项名称长度不能超过128个字符")
    private String name;

    @NotNull(message = "是否显示不能为空")
    private Boolean visible;

    private Integer sortOrder;

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
