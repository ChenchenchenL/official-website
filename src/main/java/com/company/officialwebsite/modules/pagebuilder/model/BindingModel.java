package com.company.officialwebsite.modules.pagebuilder.model;

import java.util.Map;

/**
 * BindingModel: 组件数据绑定配置模型，配置外部数据关联规则。
 */
public class BindingModel {

    private String mode;
    private String source;
    private Map<String, Object> query;

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Map<String, Object> getQuery() {
        return query;
    }

    public void setQuery(Map<String, Object> query) {
        this.query = query;
    }
}
