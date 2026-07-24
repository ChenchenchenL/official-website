package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.service.PageSchemaUpgradeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * PageSchemaUpgradeServiceImpl: 页面 Schema 升级服务实现类。
 */
@Service
public class PageSchemaUpgradeServiceImpl implements PageSchemaUpgradeService {

    private static final Logger log = LoggerFactory.getLogger(PageSchemaUpgradeServiceImpl.class);

    @Override
    public PageSchemaModel upgradeToCurrent(PageSchemaModel rawSchema) {
        if (rawSchema == null) {
            return null;
        }

        Integer version = rawSchema.getSchemaVersion();

        // 1. 若 schemaVersion 为空，归类为 Legacy v0，补齐当前标准版本 1
        if (version == null) {
            log.info("Upgrading legacy Schema (v0) for pageKey={} to current version v{}",
                    rawSchema.getPageKey(), CURRENT_SCHEMA_VERSION);
            rawSchema.setSchemaVersion(CURRENT_SCHEMA_VERSION);
            return rawSchema;
        }

        // 2. 若提交的版本为负数或 0，属于非法的 Schema 版本，拦截拒绝
        if (version < 1) {
            log.warn("Invalid Schema version v{} < 1 for pageKey={}", version, rawSchema.getPageKey());
            throw new BusinessException(ErrorCode.PAGE_SCHEMA_VERSION_UNSUPPORTED,
                    "不支持的 Schema 版本: " + version + "，版本号不能为 0 或负数");
        }

        // 3. 若提交的版本高于当前系统最高已知版本，拒绝操作
        if (version > CURRENT_SCHEMA_VERSION) {
            log.warn("Unsupported Schema version v{} > current v{} for pageKey={}",
                    version, CURRENT_SCHEMA_VERSION, rawSchema.getPageKey());
            throw new BusinessException(ErrorCode.PAGE_SCHEMA_VERSION_UNSUPPORTED,
                    "不支持的 Schema 版本: " + version + "，请升级前端编辑器或刷新后重试");
        }

        // 4. 已是当前最新版本 (v1)，直接返回
        log.debug("Schema version v{} is up to date for pageKey={}", version, rawSchema.getPageKey());
        return rawSchema;
    }
}
