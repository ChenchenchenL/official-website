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

    /**
     * 查询当前 ACTIVE 发布快照中引用目标实体的页面 ID（去重）。
     *
     * <p>该查询仅用于内容下线、删除和回滚前的强引用校验。历史或已被替代的快照
     * 不再对 Portal 输出生效，不能阻断内容生命周期操作。</p>
     *
     * @param module     业务模块标识，如 "product"
     * @param entityType 实体类型标识，如 "Product"
     * @param entityId   实体主键（字符串形式）
     * @return 当前 ACTIVE 快照引用该目标实体的页面 ID 列表
     */
    @Select("SELECT DISTINCT dependency.page_id FROM cms_page_dependency dependency "
            + "INNER JOIN cms_page_publish_snapshot snapshot "
            + "ON snapshot.id = dependency.snapshot_id "
            + "AND snapshot.page_id = dependency.page_id "
            + "AND snapshot.publish_status = 'ACTIVE' "
            + "AND snapshot.deleted_marker = 0 "
            + "WHERE dependency.deleted_marker = 0 "
            + "AND dependency.target_module = #{module} "
            + "AND dependency.target_entity_type = #{entityType} "
            + "AND (dependency.target_entity_id = #{entityId} OR dependency.target_entity_id = 'ALL')")
    List<Long> selectActivePageIdsByTarget(
            @Param("module") String module,
            @Param("entityType") String entityType,
            @Param("entityId") String entityId);

    /**
     * 查询与指定页面引用相同已发布依赖目标的全部页面 ID（去重）。
     *
     * <p>页面发布或回滚后，引用相同目标实体的页面渲染结果可能同时受影响，
     * 因此需要在同一提交后缓存失效链路中一并清理。</p>
     *
     * @param pageId 已发布或回滚的页面 ID
     * @return 关联页面 ID 列表，不保证包含传入页面 ID
     */
    @Select("SELECT DISTINCT related.page_id FROM cms_page_dependency current_dependency "
            + "INNER JOIN cms_page_dependency related "
            + "ON related.deleted_marker = 0 "
            + "AND related.target_module = current_dependency.target_module "
            + "AND related.target_entity_type = current_dependency.target_entity_type "
            + "AND related.target_entity_id = current_dependency.target_entity_id "
            + "WHERE current_dependency.deleted_marker = 0 "
            + "AND current_dependency.page_id = #{pageId}")
    List<Long> selectRelatedPageIds(@Param("pageId") Long pageId);

    /**
     * 查询指定页面依赖的所有不重复 targetModule 列表。
     * 用于发布/回滚后联动失效该页面绑定的实体模块的 Portal 列表缓存。
     */
    @Select("SELECT DISTINCT target_module FROM cms_page_dependency "
            + "WHERE deleted_marker = 0 AND page_id = #{pageId}")
    List<String> selectDistinctModulesByPageId(@Param("pageId") Long pageId);
}
