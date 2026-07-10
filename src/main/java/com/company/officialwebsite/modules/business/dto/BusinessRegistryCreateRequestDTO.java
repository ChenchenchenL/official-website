package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class BusinessRegistryCreateRequestDTO {

    @Pattern(regexp = "^[A-Za-z0-9_]{1,64}$", message = "Business code is invalid")
    private String businessCode;

    @Size(min = 1, max = 128, message = "Business name length must be between 1 and 128")
    private String businessName;

    @Positive(message = "Icon media id must be positive")
    private Long iconMediaId;

    @Size(max = 512, message = "Description length cannot exceed 512")
    private String description;

    @Pattern(regexp = "^[A-Za-z0-9_]{1,32}$", message = "Business status is invalid")
    private String businessStatus;

    @PositiveOrZero(message = "Sort order cannot be negative")
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
