package com.company.officialwebsite.modules.pagebuilder.converter;

import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDefinitionVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PageDefinitionConverter: 页面定义实体与视图展示对象互转转换器。
 */
@Component
public class PageDefinitionConverter {

    /**
     * 将 PageDefinitionEntity 转换为 PageDefinitionVO
     */
    public PageDefinitionVO toVO(PageDefinitionEntity entity) {
        if (entity == null) {
            return null;
        }
        PageDefinitionVO vo = new PageDefinitionVO();
        vo.setId(entity.getId());
        vo.setPageKey(entity.getPageKey());
        vo.setName(entity.getName());
        vo.setRoutePath(entity.getRoutePath());
        vo.setPageType(entity.getPageType());
        vo.setStatus(entity.getStatus());
        vo.setVisible(entity.getVisible());
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setSourcePageId(entity.getSourcePageId());
        vo.setSourceTemplateCode(entity.getSourceTemplateCode());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    /**
     * 将 PageDefinitionEntity 列表转换为 PageDefinitionVO 列表
     */
    public List<PageDefinitionVO> toVOList(List<PageDefinitionEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }
}
