package com.company.officialwebsite.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * IndustrySolutionCreateDTO：新增行业解决方案请求参数。
 */
public class IndustrySolutionCreateDTO {

    @NotBlank(message = "行业名称不能为空")
    @Size(max = 100, message = "行业名称最长 100 字符")
    private String name;

    @NotNull(message = "行业图标媒体 ID 不能为空")
    private Long iconMediaId;

    @NotBlank(message = "行业方案描述不能为空")
    @Size(max = 500, message = "行业方案描述最长 500 字符")
    private String description;

    @Size(max = 10, message = "典型客户标签最多 10 个")
    private List<@NotBlank(message = "客户标签不能为空") @Size(max = 30, message = "客户标签最长 30 字符") String> customerTags;

    @NotNull(message = "visible 不能为空")
    private Boolean visible;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getIconMediaId() {
        return iconMediaId;
    }

    public void setIconMediaId(Long iconMediaId) {
        this.iconMediaId = iconMediaId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getCustomerTags() {
        return customerTags;
    }

    public void setCustomerTags(List<String> customerTags) {
        this.customerTags = customerTags;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
