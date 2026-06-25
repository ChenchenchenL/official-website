package com.company.officialwebsite.modules.site.converter;

import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.media.entity.MediaAssetEntity;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import com.company.officialwebsite.modules.site.entity.AiCardEntity;
import com.company.officialwebsite.modules.site.vo.AdminAiCardVO;
import com.company.officialwebsite.modules.site.vo.PortalAiCardVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AiCardConverter：负责 Entity 与 VO 之间的模型转换。
 */
@Component
public class AiCardConverter {

    private static final Logger log = LoggerFactory.getLogger(AiCardConverter.class);

    private final MediaAssetService mediaAssetService;

    public AiCardConverter(MediaAssetService mediaAssetService) {
        this.mediaAssetService = mediaAssetService;
    }

    public AdminAiCardVO toAdminVO(AiCardEntity entity) {
        if (entity == null) {
            return null;
        }
        AdminAiCardVO vo = new AdminAiCardVO();
        vo.setId(entity.getId());
        setCommonTextFields(entity, vo::setName, vo::setEnglishName, vo::setDescription, vo::setJumpLink);
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setCreatedAt(entity.getCreatedAt());

        if (entity.getIconId() != null) {
            AdminAiCardVO.IconVO iconVO = new AdminAiCardVO.IconVO();
            iconVO.setId(entity.getIconId());
            iconVO.setUrl(resolveIconUrl(entity.getIconId(), entity.getId()));
            iconVO.setFileName(resolveIconFileName(entity.getIconId(), entity.getId()));
            vo.setIcon(iconVO);
        } else {
            vo.setIcon(null);
        }
        return vo;
    }

    public PortalAiCardVO toPortalVO(AiCardEntity entity) {
        if (entity == null) {
            return null;
        }
        PortalAiCardVO vo = new PortalAiCardVO();
        vo.setId(entity.getId());
        setCommonTextFields(entity, vo::setName, vo::setEnglishName, vo::setDescription, vo::setJumpLink);
        vo.setIconUrl(resolveIconUrl(entity.getIconId(), entity.getId()));
        return vo;
    }

    private void setCommonTextFields(AiCardEntity entity,
                                      java.util.function.Consumer<String> setName,
                                      java.util.function.Consumer<String> setEnglishName,
                                      java.util.function.Consumer<String> setDescription,
                                      java.util.function.Consumer<String> setJumpLink) {
        setName.accept(StringFieldUtils.defaultString(entity.getName()));
        setEnglishName.accept(StringFieldUtils.defaultString(entity.getEnglishName()));
        setDescription.accept(StringFieldUtils.defaultString(entity.getDescription()));
        setJumpLink.accept(StringFieldUtils.defaultString(entity.getJumpLink()));
    }

    private String resolveIconUrl(Long iconId, Long cardId) {
        if (iconId == null) {
            return "";
        }
        try {
            MediaAssetEntity asset = mediaAssetService.requireUsableImage(iconId);
            return StringFieldUtils.defaultString(asset.getPublicUrl());
        } catch (BusinessException ex) {
            log.warn("ai card icon unavailable cardId={} iconId={}", cardId, iconId);
            return "";
        }
    }

    private String resolveIconFileName(Long iconId, Long cardId) {
        if (iconId == null) {
            return "";
        }
        try {
            MediaAssetEntity asset = mediaAssetService.requireUsableImage(iconId);
            return StringFieldUtils.defaultString(asset.getOriginalFilename());
        } catch (BusinessException ex) {
            log.warn("ai card icon filename unavailable cardId={} iconId={}", cardId, iconId);
            return "";
        }
    }
}
