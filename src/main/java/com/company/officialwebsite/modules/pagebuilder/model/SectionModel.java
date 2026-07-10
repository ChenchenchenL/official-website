package com.company.officialwebsite.modules.pagebuilder.model;

import java.util.Map;

/**
 * SectionModel: 页面组件区域或组件实例配置模型。
 */
public class SectionModel {

    private String id;
    private String component;
    private Map<String, Object> props;
    private BindingModel binding;
    private Map<String, Object> style;
    private Boolean visible;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public Map<String, Object> getProps() {
        return props;
    }

    public void setProps(Map<String, Object> props) {
        this.props = props;
    }

    public BindingModel getBinding() {
        return binding;
    }

    public void setBinding(BindingModel binding) {
        this.binding = binding;
    }

    public Map<String, Object> getStyle() {
        return style;
    }

    public void setStyle(Map<String, Object> style) {
        this.style = style;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
