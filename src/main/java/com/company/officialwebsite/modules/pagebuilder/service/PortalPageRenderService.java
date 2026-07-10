package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageMetaVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PortalPageVO;

/**
 * PortalPageRenderService: 前台页面已发布快照装配及渲染服务接口。
 */
public interface PortalPageRenderService {

    /**
     * 根据路由路径加载最新 ACTIVE 快照，并解析装配其中的全部数据绑定源，生成完整的 Portal 页面渲染模型（含二级缓存）。
     *
     * @param routePath 访问路由路径
     * @return 完整的页面渲染模型
     */
    PortalPageVO renderPageByRoute(String routePath);

    /**
     * 根据页面唯一 Key 获取页面已发布的元数据配置（SEO和布局信息，不进行具体区块数据的绑定装配）。
     *
     * @param pageKey 页面唯一 Key
     * @return 页面元数据模型
     */
    PortalPageMetaVO getPageMeta(String pageKey);
}
