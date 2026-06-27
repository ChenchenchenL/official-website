package com.company.officialwebsite.modules.casecenter.vo;

import java.util.List;

/**
 * PortalCaseVO：前台标杆案例展示对象。
 */
public class PortalCaseVO {

    private String title;
    private String logoUrl;
    private String summary;
    private List<String> keywords;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }
}
