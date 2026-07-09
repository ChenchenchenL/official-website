package com.company.officialwebsite.modules.product.vo;

import com.company.officialwebsite.modules.casecenter.vo.PortalCaseVO;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Portal product detail response.
 */
public class PortalProductDetailVO {

    private Long id;
    private String title;
    private String description;
    private String content;
    private Long coverMediaId;
    private String coverUrl;
    private String seoTitle;
    private String seoDescription;
    private Boolean visible;
    private String status;
    private LocalDateTime updatedAt;
    private List<PortalCaseVO> relatedCases;
    private List<PortalIndustrySolutionVO> relatedIndustrySolutions;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getCoverMediaId() {
        return coverMediaId;
    }

    public void setCoverMediaId(Long coverMediaId) {
        this.coverMediaId = coverMediaId;
    }

    public String getCoverUrl() {
        return coverUrl;
    }

    public void setCoverUrl(String coverUrl) {
        this.coverUrl = coverUrl;
    }

    public String getSeoTitle() {
        return seoTitle;
    }

    public void setSeoTitle(String seoTitle) {
        this.seoTitle = seoTitle;
    }

    public String getSeoDescription() {
        return seoDescription;
    }

    public void setSeoDescription(String seoDescription) {
        this.seoDescription = seoDescription;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<PortalCaseVO> getRelatedCases() {
        return relatedCases;
    }

    public void setRelatedCases(List<PortalCaseVO> relatedCases) {
        this.relatedCases = relatedCases;
    }

    public List<PortalIndustrySolutionVO> getRelatedIndustrySolutions() {
        return relatedIndustrySolutions;
    }

    public void setRelatedIndustrySolutions(List<PortalIndustrySolutionVO> relatedIndustrySolutions) {
        this.relatedIndustrySolutions = relatedIndustrySolutions;
    }
}
