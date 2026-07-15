package com.company.officialwebsite.modules.content.service;

public interface ContentReferenceGuard {

    /**
     * 断言内容没有被显式引用关系或主内容依赖。
     *
     * @param contentType 资源类型标识
     * @param contentId   资源主键 ID
     */
    void assertNotReferenced(String contentType, Long contentId);

    /**
     * 断言内容未被已发布的 ACTIVE 页面或页面区块依赖引用。
     *
     * @param module     业务模块，如 "product", "casecenter"
     * @param entityType 实体类型，如 "Product", "Case", "IndustrySolution"
     * @param contentId  资源主键 ID
     */
    void assertNotReferencedByPage(String module, String entityType, Long contentId);
}
