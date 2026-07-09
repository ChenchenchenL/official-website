package com.company.officialwebsite.modules.business.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class BusinessBlockCreateRequestDTO {

    @Pattern(regexp = "^[A-Za-z0-9_]{1,64}$", message = "Block code is invalid")
    private String blockCode;

    @Size(min = 1, max = 128, message = "Block name length must be between 1 and 128")
    private String blockName;

    @Pattern(regexp = "^[A-Za-z0-9_]{1,64}$", message = "Block type is invalid")
    private String blockType;

    @Size(max = 512, message = "Description length cannot exceed 512")
    private String description;

    @Size(max = 4000, message = "Default config length cannot exceed 4000")
    private String defaultConfig;

    private Boolean visible;

    @PositiveOrZero(message = "Sort order cannot be negative")
    private Integer sortOrder;

    public String getBlockCode() {
        return blockCode;
    }

    public void setBlockCode(String blockCode) {
        this.blockCode = blockCode;
    }

    public String getBlockName() {
        return blockName;
    }

    public void setBlockName(String blockName) {
        this.blockName = blockName;
    }

    public String getBlockType() {
        return blockType;
    }

    public void setBlockType(String blockType) {
        this.blockType = blockType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDefaultConfig() {
        return defaultConfig;
    }

    public void setDefaultConfig(String defaultConfig) {
        this.defaultConfig = defaultConfig;
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
