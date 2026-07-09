package com.company.officialwebsite.modules.content.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ContentCategoryUpdateRequestDTO {

    @NotNull(message = "Version is required")
    @PositiveOrZero(message = "Version cannot be negative")
    private Integer version;

    private Long parentId;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "Category code must use letters, numbers, underscore, or hyphen")
    private String categoryCode;

    @Size(min = 1, max = 64, message = "Category name length must be between 1 and 64")
    private String categoryName;

    private Boolean visible;

    @PositiveOrZero(message = "Sort order cannot be negative")
    private Integer sortOrder;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public void setCategoryCode(String categoryCode) {
        this.categoryCode = categoryCode;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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
