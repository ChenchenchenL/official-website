package com.company.officialwebsite.modules.content.constants;

/**
 * ContentReferenceConstants：定义通用内容引用的 Referrer、Referenced 与 Reference 类型。
 */
public class ContentReferenceConstants {

    private ContentReferenceConstants() {
    }

    // Referrer Types (引用方)
    public static final String REFERRER_TYPE_PAGE_SECTION = "PAGE_SECTION";
    public static final String REFERRER_TYPE_PRODUCT_DRAFT = "PRODUCT_DRAFT";
    public static final String REFERRER_TYPE_PRODUCT_SNAPSHOT = "PRODUCT_SNAPSHOT";
    public static final String REFERRER_TYPE_CASE_DRAFT = "CASE_DRAFT";
    public static final String REFERRER_TYPE_CASE_SNAPSHOT = "CASE_SNAPSHOT";
    public static final String REFERRER_TYPE_SOLUTION_DRAFT = "SOLUTION_DRAFT";
    public static final String REFERRER_TYPE_SOLUTION_SNAPSHOT = "SOLUTION_SNAPSHOT";

    // Referenced Types (被引用实体类型)
    public static final String REFERENCED_TYPE_MEDIA = "MEDIA_ASSET";
    public static final String REFERENCED_TYPE_PRODUCT = "PRODUCT";
    public static final String REFERENCED_TYPE_CASE = "CASE";
    public static final String REFERENCED_TYPE_SOLUTION = "INDUSTRY_SOLUTION";

    // Reference Types (引用场景分类)
    public static final String REF_TYPE_COVER = "COVER";
    public static final String REF_TYPE_LOGO = "LOGO";
    public static final String REF_TYPE_GALLERY = "GALLERY";
    public static final String REF_TYPE_RICH_TEXT = "RICH_TEXT";
    public static final String REF_TYPE_RELATED_ENTITY = "RELATED_ENTITY";
}
