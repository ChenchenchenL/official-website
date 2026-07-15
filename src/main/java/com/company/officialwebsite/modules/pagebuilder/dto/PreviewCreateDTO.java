package com.company.officialwebsite.modules.pagebuilder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * PreviewCreateDTO：生成受控预览 Token 的请求体。
 * schemaHash 为必填项，后端会与数据库中当前草稿哈希比对；
 * 不一致则阻断并提示前端先保存草稿，防止预览与编辑状态不一致。
 */
public class PreviewCreateDTO {

    @NotBlank(message = "schemaHash 不能为空，请先保存草稿后再生成预览")
    @Size(max = 64, message = "schemaHash 长度不能超过 64 字符")
    private String schemaHash;

    public String getSchemaHash() {
        return schemaHash;
    }

    public void setSchemaHash(String schemaHash) {
        this.schemaHash = schemaHash;
    }
}
