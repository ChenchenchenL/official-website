package com.company.officialwebsite.modules.site.converter;

import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.entity.ResearchDirectionEntity;
import com.company.officialwebsite.modules.site.vo.AdminResearchDirectionVO;
import com.company.officialwebsite.modules.site.vo.PortalResearchDirectionVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ResearchDirectionConverter：负责重点研发方向 Entity 与 VO 的转换。
 */
@Component
public class ResearchDirectionConverter {

    private static final Logger log = LoggerFactory.getLogger(ResearchDirectionConverter.class);

    private final MediaAssetService mediaAssetService;

    public ResearchDirectionConverter(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    public AdminResearchDirectionVO toAdminVO(ResearchDirectionEntity entity) {
        if (entity == null) {
            return null;
        }
        AdminResearchDirectionVO vo = new AdminResearchDirectionVO();
        vo.setId(entity.getId());
        vo.setTitleCn(StringFieldUtils.defaultString(entity.getTitleCn()));
        vo.setTitleEn(StringFieldUtils.defaultString(entity.getTitleEn()));
        vo.setSummary(StringFieldUtils.defaultString(entity.getSummary()));
        vo.setIconMediaId(entity.getIconMediaId());
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());

        MediaAssetEntity asset = resolveIconAsset(entity.getIconMediaId(), entity.getId());
        vo.setIconUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
        return vo;
    }

    public PortalResearchDirectionVO toPortalVO(ResearchDirectionEntity entity) {
        if (entity == null) {
            return null;
        }
        PortalResearchDirectionVO vo = new PortalResearchDirectionVO();
        vo.setTitleCn(StringFieldUtils.defaultString(entity.getTitleCn()));
        vo.setTitleEn(StringFieldUtils.defaultString(entity.getTitleEn()));
        vo.setSummary(StringFieldUtils.defaultString(entity.getSummary()));
        MediaAssetEntity asset = resolveIconAsset(entity.getIconMediaId(), entity.getId());
        vo.setIconUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
        return vo;
    }

    private MediaAssetEntity resolveIconAsset(Long mediaId, Long entityId) {
        if (mediaId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (Exception ex) {
            log.warn("research direction icon unavailable directionId={} mediaId={}", entityId, mediaId);
            return null;
        }
    }
}
