package com.company.officialwebsite.modules.product.converter;

import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.product.entity.IndustrySolutionEntity;
import com.company.officialwebsite.modules.product.vo.AdminIndustrySolutionVO;
import com.company.officialwebsite.modules.product.vo.PortalIndustrySolutionVO;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * IndustrySolutionConverter：负责行业解决方案 Entity 与 VO 之间的转换。
 */
@Component
public class IndustrySolutionConverter {

    private static final Logger log = LoggerFactory.getLogger(IndustrySolutionConverter.class);

    private final MediaAssetService mediaAssetService;

    public IndustrySolutionConverter(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    public AdminIndustrySolutionVO toAdminVO(IndustrySolutionEntity entity) {
        if (entity == null) {
            return null;
        }
        AdminIndustrySolutionVO vo = new AdminIndustrySolutionVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setIconMediaId(entity.getIconMediaId());
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setCustomerTags(safeTags(entity.getCustomerTags()));
        vo.setVisible(entity.getVisible());
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());

        MediaAssetEntity asset = resolveIconAsset(entity.getIconMediaId(), entity.getId());
        vo.setIconUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
        return vo;
    }

    public PortalIndustrySolutionVO toPortalVO(IndustrySolutionEntity entity) {
        if (entity == null) {
            return null;
        }
        PortalIndustrySolutionVO vo = new PortalIndustrySolutionVO();
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setDescription(StringFieldUtils.defaultString(entity.getDescription()));
        vo.setCustomerTags(safeTags(entity.getCustomerTags()));

        MediaAssetEntity asset = resolveIconAsset(entity.getIconMediaId(), entity.getId());
        vo.setIconUrl(asset == null ? "" : StringFieldUtils.defaultString(asset.getPublicUrl()));
        return vo;
    }

    private MediaAssetEntity resolveIconAsset(Long mediaId, Long solutionId) {
        if (mediaId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(mediaId);
        } catch (BusinessException ex) {
            log.warn("industry solution icon unavailable solutionId={} mediaId={}", solutionId, mediaId);
            return null;
        }
    }

    private List<String> safeTags(List<String> tags) {
        return tags == null ? Collections.emptyList() : tags;
    }
}
