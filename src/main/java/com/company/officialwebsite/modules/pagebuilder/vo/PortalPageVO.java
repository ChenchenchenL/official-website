package com.company.officialwebsite.modules.pagebuilder.vo;

import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.SeoModel;

import java.util.List;

/**
 * PortalPageVO: 前台页面渲染的完整已发布且装配数据的模型。
 */
public class PortalPageVO {

    private String pageKey;
    private String name;
    private LayoutModel layout;
    private List<PortalSectionVO> sections;
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

    public List<PortalSectionVO> getSections() {
        return sections;
    }

    public void setSections(List<PortalSectionVO> sections) {
        this.sections = sections;
    }

    public SeoModel getSeo() {
        return seo;
    }

    public void setSeo(SeoModel seo) {
        this.seo = seo;
    }
}
