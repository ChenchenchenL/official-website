package com.company.officialwebsite.common.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DetailPreviewCreateDTO：创建产品、案例或行业方案草稿预览的请求参数。
 */
public class DetailPreviewCreateDTO {

    @NotBlank(message = "草稿哈希不能为空")
    private String draftHash;

    public String getDraftHash() {
        return draftHash;
    }

    public void setDraftHash(String draftHash) {
        this.draftHash = draftHash;
    }
}
