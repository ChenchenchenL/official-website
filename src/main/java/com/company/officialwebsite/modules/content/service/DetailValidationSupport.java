package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * DetailValidationSupport：三类详情（产品、案例、行业方案）草稿与发布通用校验支撑组件。
 */
@Component
public class DetailValidationSupport {

    private static final Logger log = LoggerFactory.getLogger(DetailValidationSupport.class);

    private final MediaAssetService mediaAssetService;

    public DetailValidationSupport(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    /**
     * 富文本 HTML Jsoup 安全清洗（防 XSS 脚本注入）。
     *
     * @param html 原始富文本 HTML
     * @return 清洗后的合法 HTML 内容
     */
    public String cleanRichTextHtml(String html) {
        if (html == null) {
            return null;
        }
        return Jsoup.clean(html, Safelist.basicWithImages());
    }

    /**
     * 校验媒体资源在库性与可用性。
     *
     * @param mediaId 媒体 ID
     */
    public void validateMediaUsable(Long mediaId) {
        if (mediaId == null || mediaId <= 0) {
            return;
        }
        try {
            mediaAssetService.requireUsableImage(mediaId);
        } catch (Exception e) {
            log.warn("detail media asset check failed mediaId={}", mediaId, e);
            throw new BusinessException(ErrorCode.MEDIA_FILE_INVALID, "引用的媒体资源不存在或不可用: " + mediaId);
        }
    }

    /**
     * 校验 SEO 标题与描述字段长度。
     *
     * @param title       SEO 标题
     * @param description SEO 描述
     */
    public void validateSeo(String title, String description) {
        if (title != null && title.length() > 128) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "SEO 标题长度不能超过 128 字符");
        }
        if (description != null && description.length() > 255) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "SEO 描述长度不能超过 255 字符");
        }
    }

    /**
     * 校验 URL 链接协议合法性。
     *
     * @param link 链接地址
     */
    public void validateLinkProtocol(String link) {
        if (link == null || link.isBlank()) {
            return;
        }
        String trimmed = link.trim();
        if (!trimmed.startsWith("http://")
                && !trimmed.startsWith("https://")
                && !trimmed.startsWith("mailto:")
                && !trimmed.startsWith("/")) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "链接协议不合法，必须以 http://, https://, mailto: 或 / 开头");
        }
    }
}
