package com.company.officialwebsite.modules.media.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class MediaAssetUpdateDTO {

    @Pattern(regexp = "LOGO|BANNER|PRODUCT|CASE|CLIENT|HONOR|ICON|OTHER", message = "媒体用途不合法")
    private String usageTag;

    @Size(max = 255, message = "替代文本不能超过255个字符")
    private String altText;

    @Size(max = 500, message = "备注不能超过500个字符")
    private String remark;

    private Integer version;

    public String getUsageTag() {
        return usageTag;
    }

    public void setUsageTag(String usageTag) {
        this.usageTag = usageTag;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
