package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.Size;

/**
 * HomeBannerButtonDTO：承载首页首屏单个 CTA 按钮的配置参数。
 */
public class HomeBannerButtonDTO {

    private Boolean enabled;

    @Size(max = 32)
    private String text;

    @Size(max = 32)
    private String targetType;

    @Size(max = 255)
    private String routePath;

    @Size(max = 64)
    private String anchorCode;

    @Size(max = 500)
    private String externalUrl;

    private Boolean openInNewTab;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getAnchorCode() {
        return anchorCode;
    }

    public void setAnchorCode(String anchorCode) {
        this.anchorCode = anchorCode;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public Boolean getOpenInNewTab() {
        return openInNewTab;
    }

    public void setOpenInNewTab(Boolean openInNewTab) {
        this.openInNewTab = openInNewTab;
    }
}
