package com.company.officialwebsite.modules.pagebuilder.vo;

import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;

/**
 * PagePreviewVO：页面预览数据返回对象，供前台渲染引擎加载草稿快照。
 * <p>
 * {@code previewToken} 字段仅在生成预览 Token 的响应中填充；
 * 通过 Token 查询预览数据时该字段为 null。
 * </p>
 */
public class PagePreviewVO {

    /** 页面唯一 Key 标识 */
    private String pageKey;

    /** 页面名称 */
    private String name;

    /** 页面完整 Schema 快照（从 Redis 中读取） */
    private PageSchemaModel schemaJson;

    /**
     * 预览令牌，仅生成 Token 接口响应时携带，查询预览数据时为 null。
     */
    private String previewToken;

    public String getPageKey() {
        return pageKey;
    }

    public void setPageKey(String pageKey) {
        this.pageKey = pageKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PageSchemaModel getSchemaJson() {
        return schemaJson;
    }

    public void setSchemaJson(PageSchemaModel schemaJson) {
        this.schemaJson = schemaJson;
    }

    public String getPreviewToken() {
        return previewToken;
    }

    public void setPreviewToken(String previewToken) {
        this.previewToken = previewToken;
    }
}
