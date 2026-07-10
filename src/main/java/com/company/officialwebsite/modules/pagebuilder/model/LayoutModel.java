package com.company.officialwebsite.modules.pagebuilder.model;

import java.util.Map;

/**
 * LayoutModel: 页面整体布局配置模型。
 */
public class LayoutModel {

    private String type;
    private Map<String, Object> responsive;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map<String, Object> getResponsive() {
        return responsive;
    }

    public void setResponsive(Map<String, Object> responsive) {
        this.responsive = responsive;
    }
}
