package com.company.officialwebsite.modules.content.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public class ContentRelationUpdateRequestDTO {

    @NotNull(message = "Version is required")
    @PositiveOrZero(message = "Version cannot be negative")
    private Integer version;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "Source type is invalid")
    private String sourceType;

    @NotNull(message = "Source id is required")
    @Positive(message = "Source id must be positive")
    private Long sourceId;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "Target type is invalid")
    private String targetType;

    @NotNull(message = "Target id is required")
    @Positive(message = "Target id must be positive")
    private Long targetId;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "Relation type is invalid")
    private String relationType;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getSourceId() {
        return sourceId;
    }

    public void setSourceId(Long sourceId) {
        this.sourceId = sourceId;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public Long getTargetId() {
        return targetId;
    }

    public void setTargetId(Long targetId) {
        this.targetId = targetId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }
}
