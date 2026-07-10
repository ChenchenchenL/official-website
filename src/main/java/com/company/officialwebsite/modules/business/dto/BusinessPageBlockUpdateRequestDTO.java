package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class BusinessPageBlockUpdateRequestDTO {

    @NotNull(message = "Version cannot be null")
    @PositiveOrZero(message = "Version cannot be negative")
    private Integer version;

    @NotNull(message = "Page id cannot be null")
    @Positive(message = "Page id must be positive")
    private Long pageId;

    @NotNull(message = "Block id cannot be null")
    @Positive(message = "Block id must be positive")
    private Long blockId;

    @Size(max = 4000, message = "Block config length cannot exceed 4000")
    private String blockConfig;

    private Boolean visible;

    @PositiveOrZero(message = "Sort order cannot be negative")
    private Integer sortOrder;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getPageId() {
        return pageId;
    }

    public void setPageId(Long pageId) {
        this.pageId = pageId;
    }

    public Long getBlockId() {
        return blockId;
    }

    public void setBlockId(Long blockId) {
        this.blockId = blockId;
    }

    public String getBlockConfig() {
        return blockConfig;
    }

    public void setBlockConfig(String blockConfig) {
        this.blockConfig = blockConfig;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
