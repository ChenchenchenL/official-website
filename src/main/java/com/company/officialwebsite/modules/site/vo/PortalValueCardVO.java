package com.company.officialwebsite.modules.site.vo;

/**
 * PortalValueCardVO：前台核心价值观卡片返回结构，仅暴露可展示字段。
 */
public class PortalValueCardVO {

    private String iconUrl;
    private String title;
    private String subtitle;
    private String description;

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
