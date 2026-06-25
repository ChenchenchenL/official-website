package com.company.officialwebsite.modules.site.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * AiCardEntity：AI战略模块化卡片实体，对应物理表 cms_ai_card。
 */
@TableName("cms_ai_card")
public class AiCardEntity extends BaseEntity {

    private String name;
    private String englishName;
    private Long iconId;
    private String description;
    private String jumpLink;
    private Boolean visible;
    private Integer sortOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEnglishName() {
        return englishName;
    }

    public void setEnglishName(String englishName) {
        this.englishName = englishName;
    }

    public Long getIconId() {
        return iconId;
    }

    public void setIconId(Long iconId) {
        this.iconId = iconId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getJumpLink() {
        return jumpLink;
    }

    public void setJumpLink(String jumpLink) {
        this.jumpLink = jumpLink;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
