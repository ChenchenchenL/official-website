package com.company.officialwebsite.modules.casecenter.converter;

import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.casecenter.entity.CaseEntity;
import com.company.officialwebsite.modules.casecenter.vo.AdminCaseVO;
import com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
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
        vo.setTitle(StringFieldUtils.defaultString(entity.getTitle()));
        vo.setSummary(StringFieldUtils.defaultString(entity.getSummary()));
        vo.setKeywords(safeKeywords(entity.getKeywords()));
        MediaAssetEntity asset = resolveLogoAsset(entity.getLogoMediaId(), entity.getId());
        vo.setLogoUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
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
}
