package com.company.officialwebsite.modules.site.vo;

/**
 * PortalHomeBannerVO：前台公开首页首屏主视觉返回对象。
 */
public class PortalHomeBannerVO {

    private String mainTitle;
    private String subTitle;
    private String backgroundImageUrl;
    private HomeBannerButtonVO primaryButton;
    private HomeBannerButtonVO secondaryButton;

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

    public String getBackgroundImageUrl() {
        return backgroundImageUrl;
    }

    public void setBackgroundImageUrl(String backgroundImageUrl) {
        this.backgroundImageUrl = backgroundImageUrl;
    }

    public HomeBannerButtonVO getPrimaryButton() {
        return primaryButton;
    }

    public void setPrimaryButton(HomeBannerButtonVO primaryButton) {
        this.primaryButton = primaryButton;
    }

    public HomeBannerButtonVO getSecondaryButton() {
        return secondaryButton;
    }

    public void setSecondaryButton(HomeBannerButtonVO secondaryButton) {
        this.secondaryButton = secondaryButton;
    }
}
