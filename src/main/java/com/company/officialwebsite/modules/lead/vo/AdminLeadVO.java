package com.company.officialwebsite.modules.lead.vo;

import java.time.LocalDateTime;

/**
 * AdminLeadVO：后台线索列表返回结构，敏感字段已脱敏。
 */
public class AdminLeadVO {

    private Long id;
    private String name;
    private String company;
    private String maskedEmail;
    private String maskedPhone;
    private String demandDescriptionPreview;
    private Integer status;
    private String statusLabel;
    private LocalDateTime submittedAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public String getMaskedEmail() {
        return maskedEmail;
    }

    public void setMaskedEmail(String maskedEmail) {
        this.maskedEmail = maskedEmail;
    }

    public String getMaskedPhone() {
        return maskedPhone;
    }

    public void setMaskedPhone(String maskedPhone) {
        this.maskedPhone = maskedPhone;
    }

    public String getDemandDescriptionPreview() {
        return demandDescriptionPreview;
    }

    public void setDemandDescriptionPreview(String demandDescriptionPreview) {
        this.demandDescriptionPreview = demandDescriptionPreview;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
