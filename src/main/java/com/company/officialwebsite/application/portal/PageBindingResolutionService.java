package com.company.officialwebsite.application.portal;

import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;

/**
 * PageBindingResolutionService: 页面数据绑定解析服务，负责在 Application 层将低代码 Schema 中配置的绑定源解析装配为业务数据对象。
 */
public interface PageBindingResolutionService {

    /**
     * 根据绑定模型配置，跨模块查询并组装对应的前台公开业务数据。
     *
     * @param binding 绑定配置信息
     * @return 对应的 Portal 端业务 VO 或聚合 VO，不满足可见性或已被删除的项会被自动过滤
     */
    Object resolveBinding(BindingModel binding);
}
