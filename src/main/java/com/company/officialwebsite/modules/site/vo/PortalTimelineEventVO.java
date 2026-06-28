package com.company.officialwebsite.modules.site.vo;

/**
 * PortalTimelineEventVO：前台公开时间轴节点返回对象。
 */
public class PortalTimelineEventVO {

    private Integer year;
    private String title;
    private String description;

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
