package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class BusinessTemplateCreateBusinessRequestDTO {

    @Pattern(regexp = "^[A-Za-z0-9_]{1,64}$", message = "Business code is invalid")
    private String businessCode;

    @Size(min = 1, max = 128, message = "Business name length must be between 1 and 128")
    private String businessName;

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
}
