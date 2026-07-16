package com.company.officialwebsite.modules.product.vo;

import java.io.Serializable;
import java.util.List;

/**
 * PortalIndustrySolutionDetailVO：前台行业解决方案公开详情 VO。
 */
public class PortalIndustrySolutionDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String name;
    private String iconUrl;
    private String description;
    private List<String> customerTags;
    private Object detailJson;

    public PortalIndustrySolutionDetailVO() {
    }

    public PortalIndustrySolutionDetailVO(Long id, String name, String iconUrl, String description, List<String> customerTags, Object detailJson) {
        this.id = id;
        this.name = name;
        this.iconUrl = iconUrl;
        this.description = description;
        this.customerTags = customerTags;
        this.detailJson = detailJson;
    }

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

    public Object getDetailJson() {
        return detailJson;
    }

    public void setDetailJson(Object detailJson) {
        this.detailJson = detailJson;
    }
}
