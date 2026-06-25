package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ClientLogoCreateRequestDTO：承载后台新增客户 Logo 的请求参数。
 */
public class ClientLogoCreateRequestDTO {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 128, message = "客户名称长度不能超过128个字符")
    private String name;

    @Size(max = 64, message = "所属行业长度不能超过64个字符")
    private String industry;

    @NotNull(message = "客户Logo不能为空")
    private Long logoId;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    @PositiveOrZero(message = "排序值不能为负数")
    private Integer sortOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public Long getLogoId() {
        return logoId;
    }

    public void setLogoId(Long logoId) {
        this.logoId = logoId;
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
