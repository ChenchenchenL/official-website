package com.company.officialwebsite.modules.site.vo;

import java.time.LocalDateTime;

/**
 * AdminSiteConfigVO：后台站点基础配置详情返回对象。
 */
public class AdminSiteConfigVO {

    private Long id;
    private Integer version;
    private String siteTitle;
    private String seoKeywords;
    private String seoDescription;
    private String brandSlogan;
    private String brandTagline;
    private Long logoLightMediaId;
    private String logoLightUrl;
    private Long logoDarkMediaId;
    private String logoDarkUrl;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

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

    public Long getLogoLightMediaId() {
        return logoLightMediaId;
    }

    public void setLogoLightMediaId(Long logoLightMediaId) {
        this.logoLightMediaId = logoLightMediaId;
    }

    public String getLogoLightUrl() {
        return logoLightUrl;
    }

    public void setLogoLightUrl(String logoLightUrl) {
        this.logoLightUrl = logoLightUrl;
    }

    public Long getLogoDarkMediaId() {
        return logoDarkMediaId;
    }

    public void setLogoDarkMediaId(Long logoDarkMediaId) {
        this.logoDarkMediaId = logoDarkMediaId;
    }

    public String getLogoDarkUrl() {
        return logoDarkUrl;
    }

    public void setLogoDarkUrl(String logoDarkUrl) {
        this.logoDarkUrl = logoDarkUrl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
