package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;

/**
 * PageSchemaUpgradeService: 页面 Schema 平滑升级服务接口。
 */
public interface PageSchemaUpgradeService {

    /** 当前后端系统支持的标准最新 Schema 版本号 */
    int CURRENT_SCHEMA_VERSION = 1;

    /**
     * 将输入的 Schema 模型按规则平滑升级至当前的 CURRENT_SCHEMA_VERSION。
     *
     * @param rawSchema 原始 Schema 对象（可能来自历史快照、旧版本草稿或前端提交）
     * @return 经过升级与默认值补全后的标准 PageSchemaModel
     */
    PageSchemaModel upgradeToCurrent(PageSchemaModel rawSchema);
}
