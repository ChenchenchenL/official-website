package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * PartnerUniversityUpdateDTO：承载合作高校更新请求参数。
 */
public class PartnerUniversityUpdateDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotBlank(message = "高校简称不能为空")
    @Size(max = 100, message = "高校简称长度不能超过100个字符")
    private String name;

    @NotBlank(message = "高校全称不能为空")
    @Size(max = 200, message = "高校全称长度不能超过200个字符")
    private String fullName;

    @NotNull(message = "高校Logo不能为空")
    private Long logoMediaId;

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

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Long getLogoMediaId() {
        return logoMediaId;
    }

    public void setLogoMediaId(Long logoMediaId) {
        this.logoMediaId = logoMediaId;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
