package com.company.officialwebsite.modules.content.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public class ContentReferenceUpdateRequestDTO {

    @NotNull(message = "Version is required")
    @PositiveOrZero(message = "Version cannot be negative")
    private Integer version;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "Referrer type is invalid")
    private String referrerType;

    @Size(min = 1, max = 128, message = "Referrer key length must be between 1 and 128")
    private String referrerKey;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,32}$", message = "Referenced type is invalid")
    private String referencedType;

    @NotNull(message = "Referenced id is required")
    @Positive(message = "Referenced id must be positive")
    private Long referencedId;

    @Pattern(regexp = "^[A-Za-z0-9_-]{1,64}$", message = "Reference type is invalid")
    private String referenceType;

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getReferrerType() {
        return referrerType;
    }

    public void setReferrerType(String referrerType) {
        this.referrerType = referrerType;
    }

    public String getReferrerKey() {
        return referrerKey;
    }

    public void setReferrerKey(String referrerKey) {
        this.referrerKey = referrerKey;
    }

    public String getReferencedType() {
        return referencedType;
    }

    public void setReferencedType(String referencedType) {
        this.referencedType = referencedType;
    }

    public Long getReferencedId() {
        return referencedId;
    }

    public void setReferencedId(Long referencedId) {
        this.referencedId = referencedId;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public void setReferenceType(String referenceType) {
        this.referenceType = referenceType;
    }
}
