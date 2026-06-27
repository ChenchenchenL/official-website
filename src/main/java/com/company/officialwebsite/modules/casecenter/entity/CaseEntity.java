package com.company.officialwebsite.modules.casecenter.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.company.officialwebsite.common.entity.BaseEntity;
import java.util.List;

/**
 * CaseEntity：标杆案例卡片配置实体，对应物理表 cms_case。
 */
@TableName(value = "cms_case", autoResultMap = true)
public class CaseEntity extends BaseEntity {

    private String title;

    private Long logoMediaId;

    private String summary;

    @TableField(value = "keywords", typeHandler = JacksonTypeHandler.class)
    private List<String> keywords;

    private Boolean visible;

    private Integer sortOrder;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getLogoMediaId() {
        return logoMediaId;
    }

    public void setLogoMediaId(Long logoMediaId) {
        this.logoMediaId = logoMediaId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
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
