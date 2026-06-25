package com.company.officialwebsite.modules.site.converter;

import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.site.entity.CapabilityCategoryEntity;
import com.company.officialwebsite.modules.site.vo.CapabilityCategoryVO;
import com.company.officialwebsite.modules.site.vo.CapabilityItemVO;
import com.company.officialwebsite.modules.site.vo.PortalCapabilityCategoryVO;
import com.company.officialwebsite.modules.site.vo.PortalCapabilityItemVO;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * CapabilityCategoryConverter：负责底座分类 Entity 与 VO 之间的模型转换。
 */
@Component
public class CapabilityCategoryConverter {

    public CapabilityCategoryVO toAdminVO(CapabilityCategoryEntity entity, List<CapabilityItemVO> items) {
        if (entity == null) {
            return null;
        }
        CapabilityCategoryVO vo = new CapabilityCategoryVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setItems(items);
        return vo;
    }

    public PortalCapabilityCategoryVO toPortalVO(CapabilityCategoryEntity entity, List<PortalCapabilityItemVO> items) {
        if (entity == null) {
            return null;
        }
        PortalCapabilityCategoryVO vo = new PortalCapabilityCategoryVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setItems(items);
        return vo;
    }
}
