package com.company.officialwebsite.modules.content.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ContentTagUpdateRequestDTO {

    @NotNull(message = "Version is required")
    @PositiveOrZero(message = "Version cannot be negative")
    private Integer version;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "Tag code must use letters, numbers, underscore, or hyphen")
    private String tagCode;

    @Size(min = 1, max = 64, message = "Tag name length must be between 1 and 64")
    private String tagName;

    @Size(max = 512, message = "Description length cannot exceed 512")
    private String description;

    private Boolean visible;

    @PositiveOrZero(message = "Sort order cannot be negative")
    private Integer sortOrder;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getTagCode() {
        return tagCode;
    }

    public void setTagCode(String tagCode) {
        this.tagCode = tagCode;
    }

    public String getTagName() {
        return tagName;
    }

    public void setTagName(String tagName) {
        this.tagName = tagName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
