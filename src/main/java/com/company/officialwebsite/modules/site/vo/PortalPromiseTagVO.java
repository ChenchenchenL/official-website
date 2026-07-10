package com.company.officialwebsite.modules.site.vo;

/**
 * PortalPromiseTagVO：前台承诺标签返回结构，仅暴露可展示字段。
 */
public class PortalPromiseTagVO {

    private String tagText;
    private String description;

    public String getTagText() {
        return tagText;
    }

    public void setTagText(String tagText) {
        this.tagText = tagText;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
