package com.company.officialwebsite.modules.site.vo;

/**
 * AdminClientLogoVO：后台客户 Logo 列表项返回对象。
 */
public class AdminClientLogoVO {

    private Long id;
    private String name;
    private String industry;
    private MediaFileVO logo;
    private Boolean visible;
    private Integer sortOrder;
    private Integer version;

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

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public MediaFileVO getLogo() {
        return logo;
    }

    public void setLogo(MediaFileVO logo) {
        this.logo = logo;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public static class MediaFileVO {

        private Long id;
        private String url;
        private String fileName;

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
}
