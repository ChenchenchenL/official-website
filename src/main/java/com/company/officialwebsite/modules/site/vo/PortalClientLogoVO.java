package com.company.officialwebsite.modules.site.vo;

/**
 * PortalClientLogoVO：前台客户 Logo 墙列表项返回对象。
 */
public class PortalClientLogoVO {

    private Long id;
    private String name;
    private String industry;
    private String logoUrl;

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

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }
}
