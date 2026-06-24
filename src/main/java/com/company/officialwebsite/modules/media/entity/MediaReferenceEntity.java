package com.company.officialwebsite.modules.media.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.officialwebsite.common.entity.BaseEntity;

/**
 * MediaReferenceEntity：记录业务对象与媒体资源之间的绑定关系。
 */
@TableName("media_reference")
public class MediaReferenceEntity extends BaseEntity {

    private Long mediaId;
    private String bizModule;
    private Long bizObjectId;
    private String bizField;

    public Long getMediaId() {
        return mediaId;
    }

    public void setMediaId(Long mediaId) {
        this.mediaId = mediaId;
    }

    public String getBizModule() {
        return bizModule;
    }

    public void setBizModule(String bizModule) {
        this.bizModule = bizModule;
    }

    public Long getBizObjectId() {
        return bizObjectId;
    }

    public void setBizObjectId(Long bizObjectId) {
        this.bizObjectId = bizObjectId;
    }

    public String getBizField() {
        return bizField;
    }

    public void setBizField(String bizField) {
        this.bizField = bizField;
    }
}
