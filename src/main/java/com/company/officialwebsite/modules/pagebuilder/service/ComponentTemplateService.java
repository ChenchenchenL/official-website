package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateUsageVO;
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

    /**
     * 查询模板在草稿和当前线上快照中的使用情况，供模板变更前进行影响评估。
     *
     * @param componentCode 组件编码
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页数量，最大 100
     * @return 分页后的模板使用概览
     */
    ComponentTemplateUsageVO getTemplateUsage(String componentCode, int pageNo, int pageSize);
}
