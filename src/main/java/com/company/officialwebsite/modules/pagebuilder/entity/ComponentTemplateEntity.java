package com.company.officialwebsite.modules.pagebuilder.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.company.officialwebsite.common.entity.BaseEntity;
import java.util.Map;

/**
 * ComponentTemplateEntity: 组件模板实体，对应物理表 cms_component_template。
 * 承载页面构建器注册的可复用组件模板、默认属性及数据绑定能力的定义。
 */
@TableName(value = "cms_component_template", autoResultMap = true)
public class ComponentTemplateEntity extends BaseEntity {

    /**
     * 组件唯一编码，如 "HeroBanner", "FeatureList"
     */
    private String componentCode;

    /**
     * 组件名称
     */
    private String name;

    /**
     * 组件分类，如 "Basic", "Business", "Feedback"
     */
    private String category;

    /**
     * 组件属性 Schema 定义 JSON (用于设计器渲染表单)
     */
    @TableField(value = "schema_definition_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> schemaDefinitionJson;

    /**
     * 组件默认属性 Props JSON
     */
    @TableField(value = "default_props_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> defaultPropsJson;

    /**
     * 数据绑定能力定义 JSON (定义支持绑定的数据源类型和查询配置)
     */
    @TableField(value = "binding_capability_json", typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> bindingCapabilityJson;

    /**
     * 组件状态：ACTIVE-活动, INACTIVE-非活动
     */
    private String status;

    /**
     * 排序值，越小越靠前
     */
    private Integer sortOrder;

    public String getComponentCode() {
        return componentCode;
    }

    public void setComponentCode(String componentCode) {
        this.componentCode = componentCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Map<String, Object> getSchemaDefinitionJson() {
        return schemaDefinitionJson;
    }

    public void setSchemaDefinitionJson(Map<String, Object> schemaDefinitionJson) {
        this.schemaDefinitionJson = schemaDefinitionJson;
    }

    public Map<String, Object> getDefaultPropsJson() {
        return defaultPropsJson;
    }

    public void setDefaultPropsJson(Map<String, Object> defaultPropsJson) {
        this.defaultPropsJson = defaultPropsJson;
    }

    public Map<String, Object> getBindingCapabilityJson() {
        return bindingCapabilityJson;
    }

    public void setBindingCapabilityJson(Map<String, Object> bindingCapabilityJson) {
        this.bindingCapabilityJson = bindingCapabilityJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
