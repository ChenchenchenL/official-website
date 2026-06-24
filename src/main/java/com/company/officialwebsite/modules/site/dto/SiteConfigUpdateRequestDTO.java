package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * SiteConfigUpdateRequestDTO：后台更新站点基础配置的请求体。
 */
public class SiteConfigUpdateRequestDTO {

    @NotNull
    @PositiveOrZero
    private Integer version;

    @NotBlank
    @Size(max = 120)
    private String siteTitle;

    @Size(max = 255)
    private String seoKeywords;

    @Size(max = 500)
    private String seoDescription;

    @NotBlank
    @Size(max = 160)
    private String brandSlogan;

    @Size(max = 255)
    private String brandTagline;

    private Long logoLightMediaId;

    private Long logoDarkMediaId;

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

    public Long getLogoDarkMediaId() {
        return logoDarkMediaId;
    }

    public void setLogoDarkMediaId(Long logoDarkMediaId) {
        this.logoDarkMediaId = logoDarkMediaId;
    }
}
