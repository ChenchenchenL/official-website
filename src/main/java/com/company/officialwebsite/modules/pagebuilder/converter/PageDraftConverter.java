package com.company.officialwebsite.modules.pagebuilder.converter;

import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDraftVO;
import org.springframework.stereotype.Component;

/**
 * PageDraftConverter：负责页面草稿 Entity 与 VO 之间的模型转换。
 * <p>
 * 遵循分层规则，所有从 Entity 到 VO 的映射集中于此，Controller 和 Service
 * 均通过本类获取 VO，不得直接暴露 Entity 对象。
 * </p>
 */
@Component
public class PageDraftConverter {

    /**
     * 将页面草稿实体转换为 VO。
     *
     * @param entity 草稿实体，可为 null
     * @return 对应的 VO，entity 为 null 时返回 null
     */
    public PageDraftVO toVO(PageDraftEntity entity) {
        if (entity == null) {
            return null;
        }
        PageDraftVO vo = new PageDraftVO();
        vo.setId(entity.getId());
        vo.setPageId(entity.getPageId());
        vo.setSchemaJson(entity.getSchemaJson());
        vo.setSchemaHash(entity.getSchemaHash());
        vo.setEditorSessionRemark(entity.getEditorSessionRemark());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }
}
