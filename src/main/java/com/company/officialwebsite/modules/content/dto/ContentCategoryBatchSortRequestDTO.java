package com.company.officialwebsite.modules.content.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ContentCategoryBatchSortRequestDTO {

    @NotEmpty(message = "Ordered category id list cannot be empty")
    private List<Long> orderedCategoryIds;

    public List<Long> getOrderedCategoryIds() {
        return orderedCategoryIds;
    }

    public void setOrderedCategoryIds(List<Long> orderedCategoryIds) {
        this.orderedCategoryIds = orderedCategoryIds;
    }
}
