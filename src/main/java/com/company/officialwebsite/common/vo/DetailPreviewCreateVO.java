package com.company.officialwebsite.common.vo;

import java.time.LocalDateTime;

/**
 * DetailPreviewCreateVO：创建详情预览 Token 统一响应 VO。
 */
public class DetailPreviewCreateVO {

    private String previewToken;
    private String previewUrl;
    private LocalDateTime expiresAt;

    public DetailPreviewCreateVO() {
    }

    public DetailPreviewCreateVO(String previewToken, String previewUrl, LocalDateTime expiresAt) {
        this.previewToken = previewToken;
        this.previewUrl = previewUrl;
        this.expiresAt = expiresAt;
    }

    public String getPreviewToken() {
        return previewToken;
    }

    public void setPreviewToken(String previewToken) {
        this.previewToken = previewToken;
    }

    public String getPreviewUrl() {
        return previewUrl;
    }

    public void setPreviewUrl(String previewUrl) {
        this.previewUrl = previewUrl;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
}
