package com.company.officialwebsite.modules.pagebuilder.service;

import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;

/**
 * PageSchemaValidationService: 页面 Schema 配置合法性校验接口。
 */
public interface PageSchemaValidationService {

    /**
     * 对传入的页面完整 Schema 进行白名单、嵌套、字段长度、链接协议及富文本安全校验。
     * 校验通过后，会对富文本字段进行 XSS 清洗，并将清洗后的内容填回 Schema 中。
     *
     * @param model 页面 Schema 配置模型
     */
    void validateSchema(PageSchemaModel model);
}
