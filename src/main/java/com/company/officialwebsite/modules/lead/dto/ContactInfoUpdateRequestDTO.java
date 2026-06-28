package com.company.officialwebsite.modules.lead.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * ContactInfoUpdateRequestDTO：承载后台更新基础联系方式的请求参数。
 */
public class ContactInfoUpdateRequestDTO {

    @NotNull(message = "版本号不能为空")
    @PositiveOrZero(message = "版本号不能为负数")
    private Integer version;

    @NotBlank(message = "联系地址不能为空")
    @Size(max = 255, message = "联系地址长度不能超过255个字符")
    private String contactAddress;

    @NotBlank(message = "商务咨询电话不能为空")
    @Size(max = 64, message = "商务咨询电话长度不能超过64个字符")
    private String businessPhone;

    @NotBlank(message = "联系邮箱不能为空")
    @Size(max = 128, message = "联系邮箱长度不能超过128个字符")
    @Email(message = "联系邮箱格式不合法")
    private String contactEmail;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getContactAddress() {
        return contactAddress;
    }

    public void setContactAddress(String contactAddress) {
        this.contactAddress = contactAddress;
    }

    public String getBusinessPhone() {
        return businessPhone;
    }

    public void setBusinessPhone(String businessPhone) {
        this.businessPhone = businessPhone;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}
