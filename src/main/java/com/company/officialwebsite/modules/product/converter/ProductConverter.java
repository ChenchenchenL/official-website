package com.company.officialwebsite.modules.product.converter;

import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.product.entity.ProductEntity;
import com.company.officialwebsite.modules.product.vo.PortalProductVO;
import com.company.officialwebsite.modules.product.vo.ProductVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * ProductConverter：负责产品配置 Entity 与 VO 之间的转换。
 */
@Component
public class ProductConverter {

    private static final Logger log = LoggerFactory.getLogger(ProductConverter.class);

    private final MediaAssetService mediaAssetService;

    public ProductConverter(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    /**
     * 将 ProductEntity 转换为管理端 VO。
     */
    public ProductVO toAdminVO(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        ProductVO vo = new ProductVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setSubTitle(StringFieldUtils.defaultString(entity.getSubTitle()));
        vo.setAbstractText(StringFieldUtils.defaultString(entity.getAbstractText()));
        vo.setStatusTag(StringFieldUtils.defaultString(entity.getStatusTag()));
        vo.setDetailLink(StringFieldUtils.defaultString(entity.getDetailLink()));
        vo.setVisible(entity.getVisible());
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());

        MediaAssetEntity asset = resolveLogoAsset(entity.getLogoId(), entity.getId());
        if (asset != null) {
            ProductVO.LogoVO logoVO = new ProductVO.LogoVO();
            logoVO.setId(entity.getLogoId());
            logoVO.setUrl(StringFieldUtils.defaultString(asset.getPublicUrl()));
            logoVO.setFileName(StringFieldUtils.defaultString(asset.getOriginalFilename()));
            vo.setLogo(logoVO);
        }
        return vo;
    }

    /**
     * 将 ProductEntity 转换为前台 VO。
     */
    public PortalProductVO toPortalVO(ProductEntity entity) {
        if (entity == null) {
            return null;
        }
        PortalProductVO vo = new PortalProductVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        MediaAssetEntity asset = resolveLogoAsset(entity.getLogoId(), entity.getId());
        vo.setLogoUrl(asset != null ? StringFieldUtils.defaultString(asset.getPublicUrl()) : "");
        vo.setSubTitle(StringFieldUtils.defaultString(entity.getSubTitle()));
        vo.setAbstractText(StringFieldUtils.defaultString(entity.getAbstractText()));
        vo.setStatusTag(StringFieldUtils.defaultString(entity.getStatusTag()));
        vo.setDetailLink(StringFieldUtils.defaultString(entity.getDetailLink()));
        return vo;
    }

    private MediaAssetEntity resolveLogoAsset(Long logoId, Long productId) {
        if (logoId == null) {
            return null;
        }
        try {
            return mediaAssetService.requireUsableImage(logoId);
        } catch (BusinessException ex) {
            log.warn("product logo unavailable productId={} logoId={}", productId, logoId);
            return null;
        }
    }
}
