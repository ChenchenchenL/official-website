package com.company.officialwebsite.modules.pagebuilder.converter;

import com.company.officialwebsite.modules.pagebuilder.entity.PageVersionEntity;
import com.company.officialwebsite.modules.pagebuilder.vo.PageVersionVO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PageVersionConverter: 页面版本历史实体与VO的转换器。
 */
@Component
public class PageVersionConverter {

    /**
     * 将 PageVersionEntity 转换为 PageVersionVO
     */
    public PageVersionVO toVO(PageVersionEntity entity) {
        if (entity == null) {
            return null;
        }
        PageVersionVO vo = new PageVersionVO();
        vo.setId(entity.getId());
        vo.setPageId(entity.getPageId());
        vo.setVersionNo(entity.getVersionNo());
        vo.setSourceType(entity.getSourceType());
        vo.setSchemaJson(entity.getSchemaJson());
        vo.setSchemaHash(entity.getSchemaHash());
        vo.setChangeSummary(entity.getChangeSummary());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    /**
     * 将 PageVersionEntity 列表转换为 PageVersionVO 列表
     */
    public List<PageVersionVO> toVOList(List<PageVersionEntity> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        return entities.stream().map(this::toVO).collect(Collectors.toList());
    }
}
