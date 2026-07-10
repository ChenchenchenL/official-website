package com.company.officialwebsite.modules.pagebuilder.vo;

import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import java.util.Map;

/**
 * PortalSectionVO: 前台页面渲染区块展示对象，包含绑定配置及装配好的业务数据。
 */
public class PortalSectionVO {

    private String id;
    private String component;
    private Map<String, Object> props;
    private Map<String, Object> style;
    private Boolean visible;
    private BindingModel binding;
    private Object bindingData;

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

    public BindingModel getBinding() {
        return binding;
    }

    public void setBinding(BindingModel binding) {
        this.binding = binding;
    }

    public Object getBindingData() {
        return bindingData;
    }

    public void setBindingData(Object bindingData) {
        this.bindingData = bindingData;
    }
}
