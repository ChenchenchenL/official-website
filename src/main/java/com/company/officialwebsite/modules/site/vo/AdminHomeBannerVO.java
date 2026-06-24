package com.company.officialwebsite.modules.site.vo;

import java.time.LocalDateTime;

/**
 * AdminHomeBannerVO：后台首页首屏主视觉配置详情返回对象。
 */
public class AdminHomeBannerVO {

    private Long id;
    private Integer version;
    private String mainTitle;
    private String subTitle;
    private Long backgroundImageMediaId;
    private String backgroundImageUrl;
    private HomeBannerButtonVO primaryButton;
    private HomeBannerButtonVO secondaryButton;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
