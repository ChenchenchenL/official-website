package com.company.officialwebsite.modules.site.converter;

import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.entity.PartnerUniversityEntity;
import com.company.officialwebsite.modules.site.vo.AdminPartnerUniversityVO;
import com.company.officialwebsite.modules.site.vo.PortalPartnerUniversityVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * PartnerUniversityConverter：负责合作高校 Entity 与 VO 的转换。
 */
@Component
public class PartnerUniversityConverter {

    private static final Logger log = LoggerFactory.getLogger(PartnerUniversityConverter.class);

    private final MediaAssetService mediaAssetService;

    public PartnerUniversityConverter(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    public AdminPartnerUniversityVO toAdminVO(PartnerUniversityEntity entity) {
        if (entity == null) {
            return null;
        }
        AdminPartnerUniversityVO vo = new AdminPartnerUniversityVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setFullName(StringFieldUtils.defaultString(entity.getFullName()));
        vo.setLogoMediaId(entity.getLogoMediaId());
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());

        MediaAssetEntity asset = resolveLogoAsset(entity.getLogoMediaId(), entity.getId());
        vo.setLogoUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
        return vo;
    }

    public PortalPartnerUniversityVO toPortalVO(PartnerUniversityEntity entity) {
        if (entity == null) {
            return null;
        }
        PortalPartnerUniversityVO vo = new PortalPartnerUniversityVO();
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setFullName(StringFieldUtils.defaultString(entity.getFullName()));
        MediaAssetEntity asset = resolveLogoAsset(entity.getLogoMediaId(), entity.getId());
        vo.setLogoUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
        return vo;
    }

    private MediaAssetEntity resolveLogoAsset(Long mediaId, Long entityId) {
        if (mediaId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (Exception ex) {
            log.warn("partner university logo unavailable universityId={} mediaId={}", entityId, mediaId);
            return null;
        }
    }
}
