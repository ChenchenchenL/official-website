package com.company.officialwebsite.modules.site.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * HomeBannerConfigEntity：记录首页首屏主视觉的单例配置。
 */
@TableName("cms_home_banner_config")
public class HomeBannerConfigEntity extends BaseEntity {

    private String configKey;
    private String mainTitle;
    private String subTitle;
    private Long backgroundImageMediaId;
    private Boolean primaryEnabled;
    private String primaryText;
    private String primaryTargetType;
    private String primaryRoutePath;
    private String primaryAnchorCode;
    private String primaryExternalUrl;
    private Boolean primaryOpenInNewTab;
    private Boolean secondaryEnabled;
    private String secondaryText;
    private String secondaryTargetType;
    private String secondaryRoutePath;
    private String secondaryAnchorCode;
    private String secondaryExternalUrl;
    private Boolean secondaryOpenInNewTab;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getMainTitle() {
        return mainTitle;
    }

    public void setMainTitle(String mainTitle) {
        this.mainTitle = mainTitle;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public Long getBackgroundImageMediaId() {
        return backgroundImageMediaId;
    }

    public void setBackgroundImageMediaId(Long backgroundImageMediaId) {
        this.backgroundImageMediaId = backgroundImageMediaId;
    }

    public Boolean getPrimaryEnabled() {
        return primaryEnabled;
    }

    public void setPrimaryEnabled(Boolean primaryEnabled) {
        this.primaryEnabled = primaryEnabled;
    }

    public String getPrimaryText() {
        return primaryText;
    }

    public void setPrimaryText(String primaryText) {
        this.primaryText = primaryText;
    }

    public String getPrimaryTargetType() {
        return primaryTargetType;
    }

    public void setPrimaryTargetType(String primaryTargetType) {
        this.primaryTargetType = primaryTargetType;
    }

    public String getPrimaryRoutePath() {
        return primaryRoutePath;
    }

    public void setPrimaryRoutePath(String primaryRoutePath) {
        this.primaryRoutePath = primaryRoutePath;
    }

    public String getPrimaryAnchorCode() {
        return primaryAnchorCode;
    }

    public void setPrimaryAnchorCode(String primaryAnchorCode) {
        this.primaryAnchorCode = primaryAnchorCode;
    }

    public String getPrimaryExternalUrl() {
        return primaryExternalUrl;
    }

    public void setPrimaryExternalUrl(String primaryExternalUrl) {
        this.primaryExternalUrl = primaryExternalUrl;
    }

    public Boolean getPrimaryOpenInNewTab() {
        return primaryOpenInNewTab;
    }

    public void setPrimaryOpenInNewTab(Boolean primaryOpenInNewTab) {
        this.primaryOpenInNewTab = primaryOpenInNewTab;
    }

    public Boolean getSecondaryEnabled() {
        return secondaryEnabled;
    }

    public void setSecondaryEnabled(Boolean secondaryEnabled) {
        this.secondaryEnabled = secondaryEnabled;
    }

    public String getSecondaryText() {
        return secondaryText;
    }

    public void setSecondaryText(String secondaryText) {
        this.secondaryText = secondaryText;
    }

    public String getSecondaryTargetType() {
        return secondaryTargetType;
    }

    public void setSecondaryTargetType(String secondaryTargetType) {
        this.secondaryTargetType = secondaryTargetType;
    }

    public String getSecondaryRoutePath() {
        return secondaryRoutePath;
    }

    public void setSecondaryRoutePath(String secondaryRoutePath) {
        this.secondaryRoutePath = secondaryRoutePath;
    }

    public String getSecondaryAnchorCode() {
        return secondaryAnchorCode;
    }

    public void setSecondaryAnchorCode(String secondaryAnchorCode) {
        this.secondaryAnchorCode = secondaryAnchorCode;
    }

    public String getSecondaryExternalUrl() {
        return secondaryExternalUrl;
    }

    public void setSecondaryExternalUrl(String secondaryExternalUrl) {
        this.secondaryExternalUrl = secondaryExternalUrl;
    }

    public Boolean getSecondaryOpenInNewTab() {
        return secondaryOpenInNewTab;
    }

    public void setSecondaryOpenInNewTab(Boolean secondaryOpenInNewTab) {
        this.secondaryOpenInNewTab = secondaryOpenInNewTab;
    }
}
