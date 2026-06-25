package com.company.officialwebsite.modules.product.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * ProductCreateDTO：新增产品时的请求参数数据传输对象。
 */
public class ProductCreateDTO {

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称最长 128 字符")
    private String name;

    @NotNull(message = "产品 Logo 媒体 ID 不能为空")
    private Long logoId;

    @Size(max = 256, message = "副标题最长 256 字符")
    private String subTitle;

    @NotBlank(message = "产品摘要不能为空")
    @Size(max = 512, message = "产品摘要最长 512 字符")
    private String abstractText;

    @Size(max = 64, message = "状态标签最长 64 字符")
    private String statusTag;

    @Size(max = 256, message = "详情跳转链接最长 256 字符")
    private String detailLink;

    @Min(value = 0, message = "visible 取值不合法")
    @Max(value = 1, message = "visible 取值不合法")
    private Integer visible;

    private Integer sortOrder;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getLogoId() {
        return logoId;
    }

    public void setLogoId(Long logoId) {
        this.logoId = logoId;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getStatusTag() {
        return statusTag;
    }

    public void setStatusTag(String statusTag) {
        this.statusTag = statusTag;
    }

    public String getDetailLink() {
        return detailLink;
    }

    public void setDetailLink(String detailLink) {
        this.detailLink = detailLink;
    }

    public Integer getVisible() {
        return visible;
    }

    public void setVisible(Integer visible) {
        this.visible = visible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
