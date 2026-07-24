package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.service.impl.PageSchemaUpgradeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PageSchemaUpgradeServiceTest {

    private PageSchemaUpgradeService upgradeService;

    @BeforeEach
    void setUp() {
        upgradeService = new PageSchemaUpgradeServiceImpl();
    }

    @Test
    @DisplayName("缺失 schemaVersion 的 Legacy v0 数据被平滑升级为 CURRENT_SCHEMA_VERSION (v1)")
    void upgradeToCurrent_nullVersion_shouldUpgradeToVersion1() {
        PageSchemaModel raw = new PageSchemaModel();
        raw.setPageKey("legacy_page");
        raw.setSchemaVersion(null);

        PageSchemaModel upgraded = upgradeService.upgradeToCurrent(raw);

        assertNotNull(upgraded);
        assertEquals(PageSchemaUpgradeService.CURRENT_SCHEMA_VERSION, upgraded.getSchemaVersion());
        assertEquals("legacy_page", upgraded.getPageKey());
    }

    @Test
    @DisplayName("当前标准版本 v1 保持不变")
    void upgradeToCurrent_version1_shouldRemainUnchanged() {
        PageSchemaModel raw = new PageSchemaModel();
        raw.setPageKey("v1_page");
        raw.setSchemaVersion(1);

        PageSchemaModel upgraded = upgradeService.upgradeToCurrent(raw);

        assertNotNull(upgraded);
        assertEquals(1, upgraded.getSchemaVersion());
    }

    @Test
    @DisplayName("未来未知高版本 (如 v99) 抛出 10012 PAGE_SCHEMA_VERSION_UNSUPPORTED 异常")
    void upgradeToCurrent_futureVersion_shouldThrow10012() {
        PageSchemaModel raw = new PageSchemaModel();
        raw.setPageKey("future_page");
        raw.setSchemaVersion(99);

        BusinessException ex = assertThrows(BusinessException.class, () -> upgradeService.upgradeToCurrent(raw));
        assertEquals(ErrorCode.PAGE_SCHEMA_VERSION_UNSUPPORTED, ex.getErrorCode());
    }

    @Test
    @DisplayName("非法的零或负数版本号 (如 0 或 -1) 抛出 10012 PAGE_SCHEMA_VERSION_UNSUPPORTED 异常")
    void upgradeToCurrent_negativeOrZeroVersion_shouldThrow10012() {
        PageSchemaModel raw0 = new PageSchemaModel();
        raw0.setPageKey("zero_page");
        raw0.setSchemaVersion(0);

        BusinessException ex0 = assertThrows(BusinessException.class, () -> upgradeService.upgradeToCurrent(raw0));
        assertEquals(ErrorCode.PAGE_SCHEMA_VERSION_UNSUPPORTED, ex0.getErrorCode());

        PageSchemaModel rawNeg = new PageSchemaModel();
        rawNeg.setPageKey("neg_page");
        rawNeg.setSchemaVersion(-1);

        BusinessException exNeg = assertThrows(BusinessException.class, () -> upgradeService.upgradeToCurrent(rawNeg));
        assertEquals(ErrorCode.PAGE_SCHEMA_VERSION_UNSUPPORTED, exNeg.getErrorCode());
    }
}
