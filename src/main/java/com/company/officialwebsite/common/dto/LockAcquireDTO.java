package com.company.officialwebsite.common.dto;

import jakarta.validation.constraints.Size;

/**
 * LockAcquireDTO：申请独占锁请求体。
 */
public class LockAcquireDTO {

    @Size(max = 255, message = "会话备注最长为255字符")
    private String editorSessionRemark;

    public LockAcquireDTO() {
    }

    public LockAcquireDTO(String editorSessionRemark) {
        this.editorSessionRemark = editorSessionRemark;
    }

    public String getEditorSessionRemark() {
        return editorSessionRemark;
    }

    public void setEditorSessionRemark(String editorSessionRemark) {
        this.editorSessionRemark = editorSessionRemark;
    }
}
