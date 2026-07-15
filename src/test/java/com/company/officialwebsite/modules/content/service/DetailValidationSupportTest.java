package com.company.officialwebsite.modules.content.service;

import com.company.officialwebsite.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class DetailValidationSupportTest {

    @Autowired
    private DetailValidationSupport detailValidationSupport;

    @Test
    @DisplayName("富文本 Jsoup XSS 恶意脚本过滤测试")
    void testCleanRichTextHtml() {
        String unsafeHtml = "<p>正文内容</p><script>alert('xss')</script><iframe src='evil.com'></iframe>";
        String cleaned = detailValidationSupport.cleanRichTextHtml(unsafeHtml);

        assertEquals("<p>正文内容</p>", cleaned.trim());
    }

    @Test
    @DisplayName("SEO 超长及非法链接协议校验")
    void testValidationRules() {
        // SEO 校验
        assertThrows(BusinessException.class, () ->
                detailValidationSupport.validateSeo("A".repeat(129), "正常描述"));

        assertThrows(BusinessException.class, () ->
                detailValidationSupport.validateSeo("正常标题", "B".repeat(256)));

        // 链接协议校验
        detailValidationSupport.validateLinkProtocol("https://company.com/page");
        detailValidationSupport.validateLinkProtocol("/product/detail");

        assertThrows(BusinessException.class, () ->
                detailValidationSupport.validateLinkProtocol("javascript:alert(1)"));
    }
}
