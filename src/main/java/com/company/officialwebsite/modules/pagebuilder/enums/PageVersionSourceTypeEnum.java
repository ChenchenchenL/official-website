package com.company.officialwebsite.modules.pagebuilder.enums;

/**
 * PageVersionSourceTypeEnum: 页面设计版本来源类型。
 */
public enum PageVersionSourceTypeEnum {
    /**
     * 手动保存备份
     */
    MANUAL_SAVE,

    /**
     * 发布时自动备份
     */
    PUBLISH_BASE,

    /**
     * 执行回滚时的快照备份
     */
    ROLLBACK_BASE
}
