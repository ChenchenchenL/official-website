package com.company.officialwebsite.modules.pagebuilder.vo;

import com.company.officialwebsite.common.response.PageResult;

/**
 * ComponentTemplateUsageVO：组件模板在当前草稿和线上 ACTIVE 快照中的引用概览。
 */
public class ComponentTemplateUsageVO {

    private String componentCode;
    private PageResult<PageDefinitionVO> activeSnapshotPages;
    private PageResult<PageDefinitionVO> draftPages;

    public String getComponentCode() {
        return componentCode;
    }

    public void setComponentCode(String componentCode) {
        this.componentCode = componentCode;
    }

    public PageResult<PageDefinitionVO> getActiveSnapshotPages() {
        return activeSnapshotPages;
    }

    public void setActiveSnapshotPages(PageResult<PageDefinitionVO> activeSnapshotPages) {
        this.activeSnapshotPages = activeSnapshotPages;
    }

    public PageResult<PageDefinitionVO> getDraftPages() {
        return draftPages;
    }

    public void setDraftPages(PageResult<PageDefinitionVO> draftPages) {
        this.draftPages = draftPages;
    }
}
