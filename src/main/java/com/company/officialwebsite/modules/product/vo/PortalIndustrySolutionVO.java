package com.company.officialwebsite.modules.product.vo;

import java.util.List;

/**
 * PortalIndustrySolutionVO：前台行业解决方案展示对象。
 */
public class PortalIndustrySolutionVO {

    private Long id;
    private String name;
    private String iconUrl;
    private String description;
    private List<String> customerTags;

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
}
