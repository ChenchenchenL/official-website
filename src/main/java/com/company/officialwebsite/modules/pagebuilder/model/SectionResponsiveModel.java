package com.company.officialwebsite.modules.pagebuilder.model;

import java.util.Map;

/**
 * SectionResponsiveModel: 页面组件区块在特定响应式断点下的覆盖配置模型。
 */
public class SectionResponsiveModel {

    private ComponentLayoutModel layout;
    private Map<String, Object> style;

    public ComponentLayoutModel getLayout() {
        return layout;
    }

    public void setLayout(ComponentLayoutModel layout) {
        this.layout = layout;
    }

    public Map<String, Object> getStyle() {
        return style;
    }

    public void setStyle(Map<String, Object> style) {
        this.style = style;
    }
}
