package com.company.officialwebsite.modules.business.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

@TableName("business_registry")
public class BusinessRegistryEntity extends BaseEntity {

    private String businessCode;
    private String businessName;
    private Long iconMediaId;
    private String description;
    private String businessStatus;
    private Integer sortOrder;

    public String getBusinessCode() {
        return businessCode;
    }

    public void setBusinessCode(String businessCode) {
        this.businessCode = businessCode;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public Long getIconMediaId() {
        return iconMediaId;
    }

    public void setIconMediaId(Long iconMediaId) {
        this.iconMediaId = iconMediaId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBusinessStatus() {
        return businessStatus;
    }

    public void setBusinessStatus(String businessStatus) {
        this.businessStatus = businessStatus;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
