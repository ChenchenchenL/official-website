package com.company.officialwebsite.modules.casecenter.vo;

import com.company.officialwebsite.modules.product.vo.PortalProductVO;
import java.util.List;

/**
 * Portal case detail response.
 */
public class PortalCaseDetailVO {

    private Long id;
    private String title;
    private String customerName;
    private String industry;
    private String background;
    private String solution;
    private String result;
    private String content;
    private Long coverMediaId;
    private String coverUrl;
    private List<String> images;
    private String status;
    private String seoTitle;
    private String seoDescription;
    private List<PortalCaseVO> recommendedCases;
    private List<PortalProductVO> relatedProducts;

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

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = background;
    }

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
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

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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

    public List<PortalCaseVO> getRecommendedCases() {
        return recommendedCases;
    }

    public void setRecommendedCases(List<PortalCaseVO> recommendedCases) {
        this.recommendedCases = recommendedCases;
    }

    public List<PortalProductVO> getRelatedProducts() {
        return relatedProducts;
    }

    public void setRelatedProducts(List<PortalProductVO> relatedProducts) {
        this.relatedProducts = relatedProducts;
    }
}
