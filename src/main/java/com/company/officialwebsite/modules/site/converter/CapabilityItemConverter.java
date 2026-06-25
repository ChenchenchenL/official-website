package com.company.officialwebsite.modules.site.converter;

import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.site.entity.CapabilityItemEntity;
import com.company.officialwebsite.modules.site.vo.CapabilityItemVO;
import com.company.officialwebsite.modules.site.vo.PortalCapabilityItemVO;
import org.springframework.stereotype.Component;

/**
 * CapabilityItemConverter：负责具体底座子项 Entity 与 VO 之间的模型转换。
 */
@Component
public class CapabilityItemConverter {

    public CapabilityItemVO toAdminVO(CapabilityItemEntity entity) {
        if (entity == null) {
            return null;
        }
        CapabilityItemVO vo = new CapabilityItemVO();
        vo.setId(entity.getId());
        vo.setCategoryId(entity.getCategoryId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    public PortalCapabilityItemVO toPortalVO(CapabilityItemEntity entity) {
        if (entity == null) {
            return null;
        }
        PortalCapabilityItemVO vo = new PortalCapabilityItemVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        return vo;
    }
}
