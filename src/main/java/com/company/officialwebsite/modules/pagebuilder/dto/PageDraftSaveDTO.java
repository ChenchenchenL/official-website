package com.company.officialwebsite.modules.pagebuilder.dto;

import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * PageDraftSaveDTO：保存页面草稿请求体，包含完整 Schema 和乐观锁版本号。
 */
public class PageDraftSaveDTO {

    /**
     * 页面完整配置的 Schema 数据，不可为空。
     */
    @NotNull(message = "页面 Schema 不能为空")
    private PageSchemaModel schemaJson;

    /**
     * 当前编辑会话备注/变更说明，可为空，最长 255 字符。
     */
    @Size(max = 255, message = "编辑会话备注不能超过 255 字符")
    private String editorSessionRemark;

    /**
     * 草稿实体的乐观锁版本号，用于并发冲突检测，不可为空。
     */
    @NotNull(message = "版本号不能为空")
    private Integer version;

    public PageSchemaModel getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(PageSchemaModel schemaJson) {
        this.schemaJson = schemaJson;
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
