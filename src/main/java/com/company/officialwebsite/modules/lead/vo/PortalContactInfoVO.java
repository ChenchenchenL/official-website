package com.company.officialwebsite.modules.lead.vo;

/**
 * PortalContactInfoVO：前台基础联系方式返回结构，仅暴露可展示字段。
 */
public class PortalContactInfoVO {

    private String contactAddress;
    private String businessPhone;
    private String contactEmail;

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
