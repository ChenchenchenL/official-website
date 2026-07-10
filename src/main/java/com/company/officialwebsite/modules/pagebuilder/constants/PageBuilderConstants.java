package com.company.officialwebsite.modules.pagebuilder.constants;

/**
 * PageBuilderConstants: 页面构建器模块的核心常量定义，如缓存键前缀等。
 */
public final class PageBuilderConstants {

    /**
     * 前台 Portal 页面完全渲染树缓存前缀。
     * 拼接格式：official:portal:page:{routePath}
     */
    public static final String PORTAL_PAGE_CACHE_PREFIX = "official:portal:page:";

    /**
     * 前台 Portal 页面基础元数据（如 SEO 信息、布局模板）缓存前缀。
     * 拼接格式：official:portal:page-meta:{pageKey}
     */
    public static final String PORTAL_PAGE_META_CACHE_PREFIX = "official:portal:page-meta:";

    /**
     * 后台设计器预览草稿缓存前缀。
     * 拼接格式：official:admin:page-preview:{pageId}
     */
    public static final String ADMIN_PAGE_PREVIEW_CACHE_PREFIX = "official:admin:page-preview:";

    private PageBuilderConstants() {
        // 防止实例化
    }
}
