package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ResearchDirectionUpdateDTO：承载重点研发方向更新请求参数。
 */
public class ResearchDirectionUpdateDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotBlank(message = "中文标题不能为空")
    @Size(max = 100, message = "中文标题长度不能超过100个字符")
    private String titleCn;

    @NotBlank(message = "英文标题不能为空")
    @Size(max = 100, message = "英文标题长度不能超过100个字符")
    private String titleEn;

    @NotBlank(message = "研发方向描述不能为空")
    @Size(max = 512, message = "研发方向描述长度不能超过512个字符")
    private String summary;

    @NotNull(message = "研发方向Icon不能为空")
    private Long iconMediaId;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTitleCn() {
        return titleCn;
    }

    public void setTitleCn(String titleCn) {
        this.titleCn = titleCn;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public Long getIconMediaId() {
        return iconMediaId;
    }

    public void setIconMediaId(Long iconMediaId) {
        this.iconMediaId = iconMediaId;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
