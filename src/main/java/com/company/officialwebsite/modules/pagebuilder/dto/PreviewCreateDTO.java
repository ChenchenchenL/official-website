package com.company.officialwebsite.modules.pagebuilder.dto;

import jakarta.validation.constraints.Size;

/**
 * PreviewCreateDTO：生成受控预览 Token 的请求体。
 * schemaHash 为可选项，传入时后端会与数据库中当前草稿哈希比对，
 * 不一致则阻断并提示前端先保存草稿，防止预览与编辑状态不一致。
 */
public class PreviewCreateDTO {

    /**
     * 可选。前端当前草稿的 SHA-256 哈希值，用于服务端比对是否需要先保存。
     * 不传则跳过比对，直接使用库中最新草稿生成预览。
     */
    @Size(max = 64, message = "schemaHash 长度不能超过 64 字符")
    private String schemaHash;

    public String getSchemaHash() {
        return schemaHash;
    }

    public void setSchemaHash(String schemaHash) {
        this.schemaHash = schemaHash;
    }
}
