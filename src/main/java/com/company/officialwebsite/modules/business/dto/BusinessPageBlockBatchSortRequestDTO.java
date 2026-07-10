package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BusinessPageBlockBatchSortRequestDTO {

    @NotEmpty(message = "Ordered page block id list cannot be empty")
    private List<Long> orderedPageBlockIds;

    public List<Long> getOrderedPageBlockIds() {
        return orderedPageBlockIds;
    }

    public void setOrderedPageBlockIds(List<Long> orderedPageBlockIds) {
        this.orderedPageBlockIds = orderedPageBlockIds;
    }
}
