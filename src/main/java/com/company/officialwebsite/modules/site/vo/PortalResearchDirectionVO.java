package com.company.officialwebsite.modules.site.vo;

/**
 * PortalResearchDirectionVO：重点研发方向前台返回对象。
 */
public class PortalResearchDirectionVO {

    private String titleCn;
    private String titleEn;
    private String summary;
    private String iconUrl;

    public String getTitleCn() {
        return titleCn;
    }

    public void setTitleCn(String titleCn) {
        this.titleCn = titleCn;
    }

    public String getTitleEn() {
        return titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }
}
