package com.company.officialwebsite.modules.casecenter.converter;

import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.vo.AdminCaseVO;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseDetailVO;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * CaseConverter：负责标杆案例 Entity 与 VO 之间的转换。
 */
@Component
public class CaseConverter {

    private static final Logger log = LoggerFactory.getLogger(CaseConverter.class);

    private final MediaAssetService mediaAssetService;

    public CaseConverter(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    public AdminCaseVO toAdminVO(CaseEntity entity) {
        if (entity == null) {
            return null;
        }
        AdminCaseVO vo = new AdminCaseVO();
        vo.setId(entity.getId());
        vo.setTitle(StringFieldUtils.defaultString(entity.getTitle()));
        vo.setLogoMediaId(entity.getLogoMediaId());
        vo.setSummary(StringFieldUtils.defaultString(entity.getSummary()));
        vo.setKeywords(safeKeywords(entity.getKeywords()));
        vo.setVisible(entity.getVisible());
        vo.setStatus(StringFieldUtils.defaultString(entity.getStatus()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        MediaAssetEntity asset = resolveLogoAsset(entity.getLogoMediaId(), entity.getId());
        vo.setLogoUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
        return vo;
    }

    public PortalCaseVO toPortalVO(CaseEntity entity) {
        if (entity == null) {
            return null;
        }
        PortalCaseVO vo = new PortalCaseVO();
        vo.setId(entity.getId());
        vo.setTitle(StringFieldUtils.defaultString(entity.getTitle()));
        vo.setSummary(StringFieldUtils.defaultString(entity.getSummary()));
        vo.setKeywords(safeKeywords(entity.getKeywords()));
        vo.setStatus(StringFieldUtils.defaultString(entity.getStatus()));
        MediaAssetEntity asset = resolveLogoAsset(entity.getLogoMediaId(), entity.getId());
        vo.setLogoUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
        return vo;
    }

    public PortalCaseDetailVO toPortalDetailVO(CaseEntity entity) {
        if (entity == null) {
            return null;
        }
        PortalCaseDetailVO vo = new PortalCaseDetailVO();
        List<String> keywords = safeKeywords(entity.getKeywords());
        MediaAssetEntity asset = resolveLogoAsset(entity.getLogoMediaId(), entity.getId());
        String coverUrl = asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl());
        String title = StringFieldUtils.defaultString(entity.getTitle());
        String summary = StringFieldUtils.defaultString(entity.getSummary());

        vo.setId(entity.getId());
        vo.setTitle(title);
        vo.setCustomerName(title);
        vo.setIndustry(keywords.isEmpty() ? "" : keywords.get(0));
        vo.setBackground(summary);
        vo.setSolution(summary);
        vo.setResult(summary);
        vo.setContent(buildCaseContent(entity));
        vo.setCoverMediaId(entity.getLogoMediaId());
        vo.setCoverUrl(coverUrl);
        vo.setImages(coverUrl.isEmpty() ? List.of() : List.of(coverUrl));
        vo.setStatus(StringFieldUtils.defaultString(entity.getStatus()));
        vo.setSeoTitle(title);
        vo.setSeoDescription(summary);
        return vo;
    }

    private MediaAssetEntity resolveLogoAsset(Long mediaId, Long caseId) {
        if (mediaId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (BusinessException ex) {
            log.warn("case logo unavailable caseId={} mediaId={}", caseId, mediaId);
            return null;
        }
    }

    private List<String> safeKeywords(List<String> keywords) {
        return keywords == null ? Collections.emptyList() : keywords;
    }

    private String buildCaseContent(CaseEntity entity) {
        StringBuilder builder = new StringBuilder();
        appendParagraph(builder, entity.getSummary());
        List<String> keywords = new ArrayList<>(safeKeywords(entity.getKeywords()));
        if (!keywords.isEmpty()) {
            appendParagraph(builder, String.join(" / ", keywords));
        }
        return builder.toString();
    }

    private void appendParagraph(StringBuilder builder, String value) {
        String text = StringFieldUtils.defaultString(value).trim();
        if (!text.isEmpty()) {
            builder.append("<p>").append(escapeHtml(text)).append("</p>");
        }
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
