package com.company.officialwebsite.modules.pagebuilder.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PageDefinitionUpdateDTO: 页面更新入参。
 */
public class PageDefinitionUpdateDTO {

    @NotBlank(message = "页面名称不能为空")
    @Size(max = 128, message = "页面名称长度不能超过128字符")
    private String name;

    @NotBlank(message = "页面访问路由路径不能为空")
    @Size(max = 255, message = "页面访问路由路径长度不能超过255字符")
    private String routePath;

    @NotNull(message = "页面前台可见性不能为空")
    private Boolean visible;

    @NotNull(message = "页面排序值不能为空")
    @Min(value = 0, message = "页面排序值不能为负数")
    private Integer sortOrder;

    @NotNull(message = "版本号不能为空")
    private Integer version;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
