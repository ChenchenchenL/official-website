package com.company.officialwebsite.modules.pagebuilder.enums;

/**
 * BindingTypeEnum: 组件数据绑定模式类型。
 */
public enum BindingTypeEnum {
    /**
     * 静态配置数据
     */
    STATIC,

    /**
     * 单个业务实体关联绑定
     */
    ENTITY,

    /**
     * 聚合或列表查询绑定
     */
    AGGREGATE
}
