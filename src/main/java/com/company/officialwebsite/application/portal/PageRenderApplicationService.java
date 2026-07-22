package com.company.officialwebsite.application.portal;

import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageMetaVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalRouteVO;

import java.util.List;

/**
 * PageRenderApplicationService: 门户前台页面装配渲染应用层服务，处理跨模块的数据查询与整合，并进行页面级二级缓存管理。
 */
public interface PageRenderApplicationService {

    /**
     * 根据路由路径加载已发布的页面配置，级联调用底层模块只读服务进行数据绑定解析与装配，返回完整的渲染模型（附带 Redis 二级缓存）。
     *
     * @param routePath 访问路由路径，例如 "/careers"
     * @return 完整装配后的页面渲染展示对象
     */
    PortalPageVO renderPageByRoute(String routePath);

    /**
     * 根据页面唯一 Key 获取页面已发布的元数据配置（SEO和布局信息，附带 Redis 二级缓存）。
     *
     * @param pageKey 页面唯一 Key，例如 "careers"
     * @return 页面元数据展示对象
     */
    PortalPageMetaVO getPageMeta(String pageKey);

    /**
     * 对草稿 Schema 执行与正式发布页面完全一致的数据绑定装配和可见性过滤，返回脱敏后的渲染模型。
     * <p>
     * 此方法为预览专用链路，有以下严格约束：
     * <ul>
     *   <li>禁止读取或写入任何 {@code official:portal:page:*} 正式缓存，预览链路完全无状态。</li>
     *   <li>输出前所有区块的 {@code binding} 节点必须被清除，不得暴露绑定配置到前端。</li>
     *   <li>不可见（visible=false）区块、隐藏或逻辑删除的绑定实体均不出现在输出中。</li>
     * </ul>
     *
     * @param pageId 页面定义 ID，用于加载草稿
     * @return 已完成绑定装配并清除 binding 元数据的渲染模型
     */
    PortalPageVO renderDraftForPreview(Long pageId);

    /**
     * 查询 Portal 启用的活动页面路由清单（供 Portal 路由注册与 Sitemap 索引）。
     *
     * @param onlyVisible 若为 true，仅返回 visible=true 的站内公开页面；若为 false，返回全部 enabled 页面
     * @return Portal 页面路由清单 VO 列表
     */
    List<PortalRouteVO> listActiveRoutes(Boolean onlyVisible);
}
