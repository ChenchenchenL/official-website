package com.company.officialwebsite.modules.pagebuilder.vo;

import java.time.LocalDateTime;

/**
 * PreviewCreateVO：生成受控预览 Token 的响应对象。
 * <p>
 * previewToken 为随机 UUID，不含任何业务数据，不得作为 JWT 或自包含 Token 使用。
 * previewUrl 为前端可直接访问的完整预览地址，由后端按配置拼接。
 * expiresAt 为 Token 在 Redis 中的过期时间（UTC+8），前端可据此提示用户。
 * </p>
 */
public class PreviewCreateVO {

    /** 随机 UUID 格式的预览令牌，前端访问预览页时须携带此值 */
    private String previewToken;

    /** 完整的前端预览访问地址 */
    private String previewUrl;

    /** Token 过期时间 */
    private LocalDateTime expiresAt;

    public PreviewCreateVO() {
    }

    public PreviewCreateVO(String previewToken, String previewUrl, LocalDateTime expiresAt) {
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
