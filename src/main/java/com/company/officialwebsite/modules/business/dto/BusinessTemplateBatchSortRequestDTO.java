package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class BusinessTemplateBatchSortRequestDTO {

    @NotEmpty(message = "Ordered template id list cannot be empty")
    private List<Long> orderedTemplateIds;

    public List<Long> getOrderedTemplateIds() {
        return orderedTemplateIds;
    }

    public void setOrderedTemplateIds(List<Long> orderedTemplateIds) {
        this.orderedTemplateIds = orderedTemplateIds;
    }
}
