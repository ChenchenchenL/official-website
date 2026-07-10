package com.company.officialwebsite.modules.pagebuilder.model;

import java.util.List;

/**
 * SeoModel: 页面 SEO 基础信息配置模型。
 */
public class SeoModel {

    private String title;
    private List<String> keywords;
    private String description;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
