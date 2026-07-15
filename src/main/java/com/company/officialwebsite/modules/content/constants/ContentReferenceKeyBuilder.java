package com.company.officialwebsite.modules.content.constants;

/**
 * ContentReferenceKeyBuilder：统一构造与解析内容引用的 referrerKey。
 *
 * <p>Key 格式规则：
 * <pre>
 *   草稿     : "{resource}_draft:{id}"      例如 "product_draft:42"
 *   快照     : "{resource}_snapshot:{id}"   例如 "product_snapshot:7"
 *   页面区块 : "page_section:{instanceId}"  例如 "page_section:section-uuid-001"
 * </pre>
 *
 * <p>所有构造方法均不允许 null 或空白参数；解析方法在格式非法时抛出
 * {@link IllegalArgumentException}，调用方应在业务边界处捕获并转换为业务异常。
 */
public final class ContentReferenceKeyBuilder {

    private static final String SEP = ":";
    private static final String PRODUCT = "product";
    private static final String CASE = "case";
    private static final String SOLUTION = "solution";
    private static final String PAGE_SECTION = "page_section";
    private static final String SUFFIX_DRAFT = "_draft";
    private static final String SUFFIX_SNAPSHOT = "_snapshot";

    private ContentReferenceKeyBuilder() {
    }

    // -----------------------------------------------------------------------
    // 构造方法
    // -----------------------------------------------------------------------

    /** 产品草稿引用 Key，格式：{@code product_draft:{draftId}}。 */
    public static String forProductDraft(Long draftId) {
        return build(PRODUCT + SUFFIX_DRAFT, draftId);
    }

    /** 产品发布快照引用 Key，格式：{@code product_snapshot:{snapshotId}}。 */
    public static String forProductSnapshot(Long snapshotId) {
        return build(PRODUCT + SUFFIX_SNAPSHOT, snapshotId);
    }

    /** 案例草稿引用 Key，格式：{@code case_draft:{draftId}}。 */
    public static String forCaseDraft(Long draftId) {
        return build(CASE + SUFFIX_DRAFT, draftId);
    }

    /** 案例发布快照引用 Key，格式：{@code case_snapshot:{snapshotId}}。 */
    public static String forCaseSnapshot(Long snapshotId) {
        return build(CASE + SUFFIX_SNAPSHOT, snapshotId);
    }

    /** 行业方案草稿引用 Key，格式：{@code solution_draft:{draftId}}。 */
    public static String forSolutionDraft(Long draftId) {
        return build(SOLUTION + SUFFIX_DRAFT, draftId);
    }

    /** 行业方案发布快照引用 Key，格式：{@code solution_snapshot:{snapshotId}}。 */
    public static String forSolutionSnapshot(Long snapshotId) {
        return build(SOLUTION + SUFFIX_SNAPSHOT, snapshotId);
    }

    /**
     * 页面区块引用 Key，格式：{@code page_section:{instanceId}}。
     *
     * @param instanceId 区块实例 ID，即 Schema 中的 {@code section.id} 字段。
     */
    public static String forPageSection(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            throw new IllegalArgumentException("instanceId must not be blank");
        }
        return PAGE_SECTION + SEP + instanceId.trim();
    }

    // -----------------------------------------------------------------------
    // 解析方法
    // -----------------------------------------------------------------------

    /**
     * 从 referrerKey 解析出数字 ID 部分（适用于草稿和快照 Key）。
     *
     * @param referrerKey 例如 {@code "product_draft:42"}
     * @return 数字 ID
     * @throws IllegalArgumentException Key 格式不合法或 ID 部分非数字
     */
    public static long parseId(String referrerKey) {
        if (referrerKey == null || referrerKey.isBlank()) {
            throw new IllegalArgumentException("referrerKey must not be blank");
        }
        int idx = referrerKey.lastIndexOf(SEP);
        if (idx < 0 || idx == referrerKey.length() - 1) {
            throw new IllegalArgumentException("Invalid referrerKey format: " + referrerKey);
        }
        String idPart = referrerKey.substring(idx + 1);
        try {
            return Long.parseLong(idPart);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Non-numeric id in referrerKey: " + referrerKey, e);
        }
    }

    /**
     * 从 referrerKey 解析出前缀部分（冒号之前的内容）。
     *
     * @param referrerKey 例如 {@code "solution_snapshot:7"}
     * @return 前缀，例如 {@code "solution_snapshot"}
     */
    public static String parsePrefix(String referrerKey) {
        if (referrerKey == null || referrerKey.isBlank()) {
            throw new IllegalArgumentException("referrerKey must not be blank");
        }
        int idx = referrerKey.lastIndexOf(SEP);
        if (idx < 0) {
            throw new IllegalArgumentException("Invalid referrerKey format: " + referrerKey);
        }
        return referrerKey.substring(0, idx);
    }

    // -----------------------------------------------------------------------
    // 私有工具
    // -----------------------------------------------------------------------

    private static String build(String prefix, Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null for prefix: " + prefix);
        }
        return prefix + SEP + id;
    }
}
