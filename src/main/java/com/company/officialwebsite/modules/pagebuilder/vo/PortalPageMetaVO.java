package com.company.officialwebsite.modules.pagebuilder.vo;

import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.SeoModel;

/**
 * PortalPageMetaVO: 前台页面渲染的元数据（布局和SEO）展示模型。
 */
public class PortalPageMetaVO {

    private String pageKey;
    private String name;
    private LayoutModel layout;
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

    public SeoModel getSeo() {
        return seo;
    }

    public void setSeo(SeoModel seo) {
        this.seo = seo;
    }
}
