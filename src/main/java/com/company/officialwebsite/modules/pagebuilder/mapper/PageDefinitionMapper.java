package com.company.officialwebsite.modules.pagebuilder.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.officialwebsite.modules.pagebuilder.dto.PageRouteProjection;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * PageDefinitionMapper: 页面定义数据访问接口。
 */
public interface PageDefinitionMapper extends BaseMapper<PageDefinitionEntity> {

    /**
     * 根据页面 ID 集合查询页面的路由路径和 pageKey，用于缓存联动失效时组装缓存 key。
     * 仅返回未逻辑删除的页面，软删除后无需清理缓存（缓存已在删除时失效）。
     *
     * @param ids 页面 ID 集合，不能为空
     * @return 页面路由投影列表
     */
    @Select("<script>"
            + "SELECT id, page_key, route_path FROM cms_page_definition "
            + "WHERE deleted_marker = 0 AND id IN "
            + "<foreach collection='ids' item='id' open='(' separator=',' close=')'>"
            + "#{id}"
            + "</foreach>"
            + "</script>")
    List<PageRouteProjection> selectRoutesByPageIds(@Param("ids") Collection<Long> ids);
}
