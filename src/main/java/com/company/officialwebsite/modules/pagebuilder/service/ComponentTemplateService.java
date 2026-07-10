package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import java.util.List;

/**
 * ComponentTemplateService: 组件物料模板管理服务接口。
 */
public interface ComponentTemplateService {

    /**
     * 获取全部已启用的组件模板列表。
     *
     * @return 组件模板VO列表
     */
    List<ComponentTemplateVO> getActiveTemplates();

    /**
     * 根据组件唯一编码获取组件模板详情。
     *
     * @param componentCode 组件编码
     * @return 组件模板VO详情
     */
    ComponentTemplateVO getTemplateByCode(String componentCode);
}
