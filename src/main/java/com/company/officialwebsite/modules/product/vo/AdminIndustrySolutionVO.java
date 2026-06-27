package com.company.officialwebsite.modules.product.vo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AdminIndustrySolutionVO：后台行业解决方案列表展示对象。
 */
public class AdminIndustrySolutionVO {

    private Long id;
    private String name;
    private Long iconMediaId;
    private String iconUrl;
    private String description;
    private List<String> customerTags;
    private Boolean visible;
    private Integer sortOrder;
    private Integer version;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
