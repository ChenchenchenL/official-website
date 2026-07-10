package com.company.officialwebsite.modules.pagebuilder.vo;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * ComponentTemplateVO：组件模板详情返回对象。
 */
public class ComponentTemplateVO {

    private Long id;
    private String componentCode;
    private String name;
    private String category;
    private Map<String, Object> schemaDefinitionJson;
    private Map<String, Object> defaultPropsJson;
    private Map<String, Object> bindingCapabilityJson;
    private String status;
    private Integer sortOrder;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
