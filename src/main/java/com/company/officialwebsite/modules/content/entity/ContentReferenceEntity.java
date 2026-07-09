package com.company.officialwebsite.modules.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

@TableName("content_reference")
public class ContentReferenceEntity extends BaseEntity {

    private String referrerType;
    private String referrerKey;
    private String referencedType;
    private Long referencedId;
    private String referenceType;

    public String getReferrerType() {
        return referrerType;
    }

    public void setReferrerType(String referrerType) {
        this.referrerType = referrerType;
    }

    public String getReferrerKey() {
        return referrerKey;
    }

    public void setReferrerKey(String referrerKey) {
        this.referrerKey = referrerKey;
    }

    public String getReferencedType() {
        return referencedType;
    }

    public void setReferencedType(String referencedType) {
        this.referencedType = referencedType;
    }

    public Long getReferencedId() {
        return referencedId;
    }

    public void setReferencedId(Long referencedId) {
        this.referencedId = referencedId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
}
