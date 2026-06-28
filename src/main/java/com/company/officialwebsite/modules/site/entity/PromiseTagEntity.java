package com.company.officialwebsite.modules.site.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * PromiseTagEntity：记录"我们的承诺"底部的单条承诺特性标签。
 */
@TableName("cms_promise_tag")
public class PromiseTagEntity extends BaseEntity {

    private String tagText;
    private Integer sortOrder;

    public String getTagText() {
        return tagText;
    }

    public void setTagText(String tagText) {
        this.tagText = tagText;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
