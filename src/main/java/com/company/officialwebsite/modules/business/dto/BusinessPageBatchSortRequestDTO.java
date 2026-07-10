package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BusinessPageBatchSortRequestDTO {

    @NotEmpty(message = "Ordered page id list cannot be empty")
    private List<Long> orderedPageIds;

    public List<Long> getOrderedPageIds() {
        return orderedPageIds;
    }

    public void setOrderedPageIds(List<Long> orderedPageIds) {
        this.orderedPageIds = orderedPageIds;
    }
}
