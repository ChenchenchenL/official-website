package com.company.officialwebsite.modules.site.vo;

/**
 * PortalSiteConfigVO：前台公开站点基础配置返回对象。
 */
public class PortalSiteConfigVO {

    private String siteTitle;
    private String seoKeywords;
    private String seoDescription;
    private String brandSlogan;
    private String brandTagline;
    private String logoLightUrl;
    private String logoDarkUrl;

    public String getSiteTitle() {
        return siteTitle;
    }

    public void setSiteTitle(String siteTitle) {
        this.siteTitle = siteTitle;
    }

    public String getSeoKeywords() {
        return seoKeywords;
    }

    public void setSeoKeywords(String seoKeywords) {
        this.seoKeywords = seoKeywords;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    public String getBrandSlogan() {
        return brandSlogan;
    }

    public void setBrandSlogan(String brandSlogan) {
        this.brandSlogan = brandSlogan;
    }

    public String getBrandTagline() {
        return brandTagline;
    }

    public void setBrandTagline(String brandTagline) {
        this.brandTagline = brandTagline;
    }

    public String getLogoLightUrl() {
        return logoLightUrl;
    }

    public void setLogoLightUrl(String logoLightUrl) {
        this.logoLightUrl = logoLightUrl;
    }

    public String getLogoDarkUrl() {
        return logoDarkUrl;
    }

    public void setLogoDarkUrl(String logoDarkUrl) {
        this.logoDarkUrl = logoDarkUrl;
    }
}
