package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BusinessBlockBatchSortRequestDTO {

    @NotEmpty(message = "Ordered block id list cannot be empty")
    private List<Long> orderedBlockIds;

    public List<Long> getOrderedBlockIds() {
        return orderedBlockIds;
    }

    public void setOrderedBlockIds(List<Long> orderedBlockIds) {
        this.orderedBlockIds = orderedBlockIds;
    }
}
