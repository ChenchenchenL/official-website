package com.company.officialwebsite.modules.site.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * SiteConfigEntity：站点基础配置单例聚合的持久化对象。
 */
@TableName("cms_site_config")
public class SiteConfigEntity extends BaseEntity {

    private String configKey;
    private String siteTitle;
    private String seoKeywords;
    private String seoDescription;
    private String brandSlogan;
    private String brandTagline;
    private Long logoLightMediaId;
    private Long logoDarkMediaId;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
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
