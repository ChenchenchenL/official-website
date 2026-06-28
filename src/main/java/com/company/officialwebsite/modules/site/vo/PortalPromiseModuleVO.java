package com.company.officialwebsite.modules.site.vo;

import java.util.List;

/**
 * PortalPromiseModuleVO：前台"我们的承诺"模块聚合返回结构。
 */
public class PortalPromiseModuleVO {

    private String content;
    private List<PortalPromiseTagVO> tags;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<PortalPromiseTagVO> getTags() {
        return tags;
    }

    public void setTags(List<PortalPromiseTagVO> tags) {
        this.tags = tags;
    }
}
