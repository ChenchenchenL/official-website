package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.common.enums.EditorResourceTypeEnum;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.content.dto.DetailPreviewTokenData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class DetailPreviewTokenServiceTest {

    @Autowired
    private DetailPreviewTokenService detailPreviewTokenService;

    @Test
    @DisplayName("详情预览 Token 创建、解析与主动撤销生命周期测试")
    void testPreviewTokenLifecycle() {
        String token = detailPreviewTokenService.createToken(
                EditorResourceTypeEnum.PRODUCT, 100L, "hash123", "admin_tester"
        );
        assertNotNull(token);

        DetailPreviewTokenData data = detailPreviewTokenService.resolveToken(token);
        assertNotNull(data);
        assertEquals(EditorResourceTypeEnum.PRODUCT, data.getResourceType());
        assertEquals(100L, data.getResourceId());
        assertEquals("hash123", data.getDraftHash());
        assertEquals("admin_tester", data.getCreatedBy());

        // 主动撤销 Token
        detailPreviewTokenService.revokeToken(token);

        // 再次解析应抛出 DETAIL_PREVIEW_TOKEN_EXPIRED 异常
        assertThrows(BusinessException.class, () -> detailPreviewTokenService.resolveToken(token));
    }

    @Test
    @DisplayName("详情预览 Token 拒绝页面资源类型和不完整绑定字段")
    void createToken_shouldRejectUnsupportedResourceTypeAndIncompleteBindings() {
        assertInvalidTokenBinding(EditorResourceTypeEnum.PAGE, 100L, "hash", "admin");
        assertInvalidTokenBinding(EditorResourceTypeEnum.PRODUCT, null, "hash", "admin");
        assertInvalidTokenBinding(EditorResourceTypeEnum.PRODUCT, 100L, "", "admin");
        assertInvalidTokenBinding(EditorResourceTypeEnum.PRODUCT, 100L, "hash", " ");
    }

    private void assertInvalidTokenBinding(
            EditorResourceTypeEnum resourceType,
            Long resourceId,
            String draftHash,
            String createdBy) {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                detailPreviewTokenService.createToken(resourceType, resourceId, draftHash, createdBy));
        assertEquals(com.company.officialwebsite.common.enums.ErrorCode.COMMON_PARAM_INVALID, exception.getErrorCode());
    }
}
