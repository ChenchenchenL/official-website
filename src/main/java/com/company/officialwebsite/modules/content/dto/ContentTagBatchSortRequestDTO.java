package com.company.officialwebsite.modules.content.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class ContentTagBatchSortRequestDTO {

    @NotEmpty(message = "Ordered tag id list cannot be empty")
    private List<Long> orderedTagIds;

    public List<Long> getOrderedTagIds() {
        return orderedTagIds;
    }

    public void setOrderedTagIds(List<Long> orderedTagIds) {
        this.orderedTagIds = orderedTagIds;
    }
}
