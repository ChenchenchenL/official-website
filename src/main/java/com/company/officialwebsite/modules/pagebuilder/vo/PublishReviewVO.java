package com.company.officialwebsite.modules.pagebuilder.vo;

import java.util.List;

/**
 * PublishReviewVO: 发布前审阅综合概览视图对象。
 */
public class PublishReviewVO {

    private Long pageId;
    private String pageName;
    private Integer draftVersion;
    private String draftSchemaHash;
    private Integer activeVersion;
    private String activeSchemaHash;
    private int draftSectionCount;
    private int activeSectionCount;
    private List<String> bindingSources;
    private boolean validationPassed;
    private String validationErrorMessage;
    private List<SchemaDiffItemVO> diffItems;

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public String getPageName() {
        return pageName;
    }

    public void setPageName(String pageName) {
        this.pageName = pageName;
    }

    public Integer getDraftVersion() {
        return draftVersion;
    }

    public void setDraftVersion(Integer draftVersion) {
        this.draftVersion = draftVersion;
    }

    public String getDraftSchemaHash() {
        return draftSchemaHash;
    }

    public void setDraftSchemaHash(String draftSchemaHash) {
        this.draftSchemaHash = draftSchemaHash;
    }

    public Integer getActiveVersion() {
        return activeVersion;
    }

    public void setActiveVersion(Integer activeVersion) {
        this.activeVersion = activeVersion;
    }

    public String getActiveSchemaHash() {
        return activeSchemaHash;
    }

    public void setActiveSchemaHash(String activeSchemaHash) {
        this.activeSchemaHash = activeSchemaHash;
    }

    public int getDraftSectionCount() {
        return draftSectionCount;
    }

    public void setDraftSectionCount(int draftSectionCount) {
        this.draftSectionCount = draftSectionCount;
    }

    public int getActiveSectionCount() {
        return activeSectionCount;
    }

    public void setActiveSectionCount(int activeSectionCount) {
        this.activeSectionCount = activeSectionCount;
    }

    public List<String> getBindingSources() {
        return bindingSources;
    }

    public void setBindingSources(List<String> bindingSources) {
        this.bindingSources = bindingSources;
    }

    public boolean isValidationPassed() {
        return validationPassed;
    }

    public void setValidationPassed(boolean validationPassed) {
        this.validationPassed = validationPassed;
    }

    public String getValidationErrorMessage() {
        return validationErrorMessage;
    }

    public void setValidationErrorMessage(String validationErrorMessage) {
        this.validationErrorMessage = validationErrorMessage;
    }

    public List<SchemaDiffItemVO> getDiffItems() {
        return diffItems;
    }

    public void setDiffItems(List<SchemaDiffItemVO> diffItems) {
        this.diffItems = diffItems;
    }
}
