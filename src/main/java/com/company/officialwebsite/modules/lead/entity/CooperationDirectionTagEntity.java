package com.company.officialwebsite.modules.lead.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * CooperationDirectionTagEntity：记录"联系我们"页面左侧合作方向标签区中的单条文本标签。
 */
@TableName("cms_cooperation_direction_tag")
public class CooperationDirectionTagEntity extends BaseEntity {

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
