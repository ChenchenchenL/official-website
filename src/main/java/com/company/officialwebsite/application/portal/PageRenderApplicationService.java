package com.company.officialwebsite.application.portal;

import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageMetaVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageVO;

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
}
