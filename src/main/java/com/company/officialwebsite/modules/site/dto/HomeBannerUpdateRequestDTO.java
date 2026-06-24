package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * HomeBannerUpdateRequestDTO：后台更新首页首屏主视觉配置的请求体。
 */
public class HomeBannerUpdateRequestDTO {

    @NotNull
    @PositiveOrZero
    private Integer version;

    @NotBlank
    @Size(max = 120)
    private String mainTitle;

    @Size(max = 500)
    private String subTitle;

    private Long backgroundImageMediaId;

    @Valid
    @NotNull
    private HomeBannerButtonDTO primaryButton;

    @Valid
    @NotNull
    private HomeBannerButtonDTO secondaryButton;

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

    public HomeBannerButtonDTO getPrimaryButton() {
        return primaryButton;
    }

    public void setPrimaryButton(HomeBannerButtonDTO primaryButton) {
        this.primaryButton = primaryButton;
    }

    public HomeBannerButtonDTO getSecondaryButton() {
        return secondaryButton;
    }

    public void setSecondaryButton(HomeBannerButtonDTO secondaryButton) {
        this.secondaryButton = secondaryButton;
    }
}
