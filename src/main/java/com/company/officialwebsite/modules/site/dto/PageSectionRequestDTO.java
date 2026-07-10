package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class PageSectionRequestDTO {

    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotBlank(message = "页面编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_-]{0,63}$", message = "页面编码格式不合法")
    private String pageCode;

    @NotBlank(message = "区块编码不能为空")
    @Pattern(regexp = "^[a-z][a-z0-9_-]{0,63}$", message = "区块编码格式不合法")
    private String sectionCode;

    @NotBlank(message = "标题不能为空")
    @Size(max = 160, message = "标题不能超过160个字符")
    private String title;

    @Size(max = 255, message = "副标题不能超过255个字符")
    private String subtitle;

    @Size(max = 1000, message = "描述不能超过1000个字符")
    private String description;

    @Size(max = 20000, message = "内容JSON不能超过20000个字符")
    private String contentJson;

    @PositiveOrZero(message = "排序值不能为负数")
    private Integer sortOrder;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    @NotBlank(message = "发布状态不能为空")
    @Pattern(regexp = "DRAFT|PUBLISHED|OFFLINE", message = "发布状态不合法")
    private String status;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getPageCode() {
        return pageCode;
    }

    public void setPageCode(String pageCode) {
        this.pageCode = pageCode;
    }

    public String getSectionCode() {
        return sectionCode;
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode = sectionCode;
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

    public String getContentJson() {
        return contentJson;
    }

    public void setContentJson(String contentJson) {
        this.contentJson = contentJson;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
