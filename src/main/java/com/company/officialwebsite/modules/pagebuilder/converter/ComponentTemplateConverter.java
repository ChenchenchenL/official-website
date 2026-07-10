package com.company.officialwebsite.modules.pagebuilder.converter;

import com.company.officialwebsite.modules.pagebuilder.entity.ComponentTemplateEntity;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * ComponentTemplateConverter：负责组件模板 Entity 与 VO 之间的模型转换。
 */
@Component
public class ComponentTemplateConverter {

    public ComponentTemplateVO toVO(ComponentTemplateEntity entity) {
        if (entity == null) {
            return null;
        }
        ComponentTemplateVO vo = new ComponentTemplateVO();
        vo.setId(entity.getId());
        vo.setComponentCode(entity.getComponentCode());
        vo.setName(entity.getName());
        vo.setCategory(entity.getCategory());
        vo.setSchemaDefinitionJson(entity.getSchemaDefinitionJson());
        vo.setDefaultPropsJson(entity.getDefaultPropsJson());
        vo.setBindingCapabilityJson(entity.getBindingCapabilityJson());
        vo.setStatus(entity.getStatus());
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    public List<ComponentTemplateVO> toVOList(List<ComponentTemplateEntity> entities) {
        if (entities == null) {
            return new ArrayList<>();
        }
        List<ComponentTemplateVO> vos = new ArrayList<>(entities.size());
        for (ComponentTemplateEntity entity : entities) {
            vos.add(toVO(entity));
        }
        return vos;
    }
}
