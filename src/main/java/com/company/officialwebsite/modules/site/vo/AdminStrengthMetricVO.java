package com.company.officialwebsite.modules.site.vo;

import java.time.LocalDateTime;

/**
 * AdminStrengthMetricVO：后台企业实力核心指标详情返回对象。
 * 包含图标信息（id + url + 文件名）、版本号及审计时间，用于后台列表和编辑回显。
 */
public class AdminStrengthMetricVO {

    private Long id;
    private String metricValue;
    private String label;

    /** 图标媒体文件ID，可能为 null（无图标指标）。 */
    private Long iconId;

    /** 图标访问URL，iconId 为 null 时返回空字符串。 */
    private String iconUrl;

    /** 图标原始文件名，iconId 为 null 时返回空字符串。 */
    private String iconFileName;

    private Boolean visible;
    private Integer sortOrder;
    private Integer version;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMetricValue() {
        return metricValue;
    }

    public void setMetricValue(String metricValue) {
        this.metricValue = metricValue;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getIconId() {
        return iconId;
    }

    public void setIconId(Long iconId) {
        this.iconId = iconId;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getIconFileName() {
        return iconFileName;
    }

    public void setIconFileName(String iconFileName) {
        this.iconFileName = iconFileName;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
