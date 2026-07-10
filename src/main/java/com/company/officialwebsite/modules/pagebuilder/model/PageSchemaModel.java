package com.company.officialwebsite.modules.pagebuilder.model;

import java.util.List;

/**
 * PageSchemaModel: 页面完整配置的 Schema 协议模型。
 */
public class PageSchemaModel {

    private String pageKey;
    private String name;
    private LayoutModel layout;
    private List<SectionModel> sections;
    private SeoModel seo;

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LayoutModel getLayout() {
        return layout;
    }

    public void setLayout(LayoutModel layout) {
        this.layout = layout;
    }

    public List<SectionModel> getSections() {
        return sections;
    }

    public void setSections(List<SectionModel> sections) {
        this.sections = sections;
    }

    public SeoModel getSeo() {
        return seo;
    }

    public void setSeo(SeoModel seo) {
        this.seo = seo;
    }
}
