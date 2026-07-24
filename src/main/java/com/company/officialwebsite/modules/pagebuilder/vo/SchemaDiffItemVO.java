package com.company.officialwebsite.modules.pagebuilder.vo;

/**
 * SchemaDiffItemVO: 组件或 Schema 变更项明细视图对象。
 */
public class SchemaDiffItemVO {

    private String path;
    private String componentId;
    private String componentCode;
    private String changeType; // "ADDED", "REMOVED", "MODIFIED"
    private String fieldName;
    private Object oldValue;
    private Object newValue;

    public SchemaDiffItemVO() {
    }

    public SchemaDiffItemVO(String path, String componentId, String componentCode, String changeType, String fieldName, Object oldValue, Object newValue) {
        this.path = path;
        this.componentId = componentId;
        this.componentCode = componentCode;
        this.changeType = changeType;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
    }

    public SchemaDiffItemVO(String path, String componentId, String componentCode, com.company.officialwebsite.modules.pagebuilder.enums.SchemaChangeTypeEnum changeTypeEnum, String fieldName, Object oldValue, Object newValue) {
        this(path, componentId, componentCode, changeTypeEnum != null ? changeTypeEnum.name() : null, fieldName, oldValue, newValue);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getComponentCode() {
        return componentCode;
    }

    public void setComponentCode(String componentCode) {
        this.componentCode = componentCode;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public Object getOldValue() {
        return oldValue;
    }

    public void setOldValue(Object oldValue) {
        this.oldValue = oldValue;
    }

    public Object getNewValue() {
        return newValue;
    }

    public void setNewValue(Object newValue) {
        this.newValue = newValue;
    }
}
