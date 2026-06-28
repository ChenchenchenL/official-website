package com.company.officialwebsite.modules.site.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * PromiseContentEntity：承载"我们的承诺"主体宣导长文本的单例配置记录。
 */
@TableName("cms_promise_content")
public class PromiseContentEntity extends BaseEntity {

    private String configKey;
    private String content;

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
