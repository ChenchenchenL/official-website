package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * TimelineEventCreateRequestDTO：承载后台新增时间轴节点的请求参数。
 */
public class TimelineEventCreateRequestDTO {

    @NotNull(message = "年份不能为空")
    private Integer year;

    @NotBlank(message = "标题不能为空")
    @Size(max = 128, message = "标题长度不能超过128个字符")
    private String title;

    @NotBlank(message = "描述不能为空")
    @Size(max = 512, message = "描述长度不能超过512个字符")
    private String description;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
