package com.company.officialwebsite.common.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DetailDraftSaveDTO：保存产品/案例/行业方案详情草稿请求 DTO。
 */
public class DetailDraftSaveDTO {

    @NotNull(message = "草稿内容不能为空")
    private Object draft;

    @Size(max = 255, message = "编辑会话备注最长为255字符")
    private String editorSessionRemark;

    @NotNull(message = "乐观锁版本号不能为空")
    private Integer version;

    public DetailDraftSaveDTO() {
    }

    public DetailDraftSaveDTO(Object draft, String editorSessionRemark, Integer version) {
        this.draft = draft;
        this.editorSessionRemark = editorSessionRemark;
        this.version = version;
    }

    public Object getDraft() {
        return draft;
    }

    public void setDraft(Object draft) {
        this.draft = draft;
    }

    public String getEditorSessionRemark() {
        return editorSessionRemark;
    }

    public void setEditorSessionRemark(String editorSessionRemark) {
        this.editorSessionRemark = editorSessionRemark;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
