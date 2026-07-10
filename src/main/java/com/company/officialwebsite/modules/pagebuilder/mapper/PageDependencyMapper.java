package com.company.officialwebsite.modules.pagebuilder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDependencyEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * PageDependencyMapper: 页面数据依赖数据访问接口。
 */
public interface PageDependencyMapper extends BaseMapper<PageDependencyEntity> {

    /**
     * 根据目标实体查找依赖该实体的所有页面 ID（去重）。
     * 用于缓存联动失效：实体变更时，找出所有引用了该实体的已发布页面。
     *
     * @param module     业务模块标识，如 "product"
     * @param entityType 实体类型标识，如 "Product"
     * @param entityId   实体主键（字符串形式）
     * @return 依赖该实体的页面 ID 列表（已去重）
     */
    @Select("SELECT DISTINCT page_id FROM cms_page_dependency "
            + "WHERE deleted_marker = 0 "
            + "AND target_module = #{module} "
            + "AND target_entity_type = #{entityType} "
            + "AND (target_entity_id = #{entityId} OR target_entity_id = 'ALL')")
    List<Long> selectPageIdsByTarget(
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId);
}
