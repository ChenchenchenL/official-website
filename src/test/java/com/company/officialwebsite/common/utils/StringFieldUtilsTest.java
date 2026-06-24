package com.company.officialwebsite.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * StringFieldUtilsTest：验证统一字符串标准化工具的空白处理语义。
 */
class StringFieldUtilsTest {

    @Test
    void trimHelpers_shouldNormalizeBlankValuesConsistently() {
        assertNull(StringFieldUtils.trimToNull("   "));
        assertEquals("", StringFieldUtils.trimToEmpty("   "));
        assertEquals("", StringFieldUtils.defaultString(null));
        assertTrue(StringFieldUtils.isBlank("   "));
        assertFalse(StringFieldUtils.isBlank(" value "));
    }
}
