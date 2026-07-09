package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BusinessRegistryBatchSortRequestDTO {

    @NotEmpty(message = "Ordered business id list cannot be empty")
    private List<Long> orderedBusinessIds;

    public List<Long> getOrderedBusinessIds() {
        return orderedBusinessIds;
    }

    public void setOrderedBusinessIds(List<Long> orderedBusinessIds) {
        this.orderedBusinessIds = orderedBusinessIds;
    }
}
