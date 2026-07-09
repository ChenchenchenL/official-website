package com.company.officialwebsite.modules.casecenter.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * CaseCreateDTO：新增标杆案例请求参数。
 */
public class CaseCreateDTO {

    @NotBlank(message = "项目标题不能为空")
    @Size(max = 128, message = "项目标题最长 128 字符")
    private String title;

    @NotNull(message = "案例封面媒体 ID 不能为空")
    private Long logoMediaId;

    @NotBlank(message = "成效摘要不能为空")
    @Size(max = 512, message = "成效摘要最长 512 字符")
    private String summary;

    @Size(max = 10, message = "核心关键词标签最多 10 个")
    private List<@NotBlank(message = "关键词不能为空") @Size(max = 30, message = "关键词最长 30 字符") String> keywords;

    @NotNull(message = "visible 不能为空")
    private Boolean visible;

    @Size(max = 32, message = "内容状态最长 32 字符")
    private String status;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getLogoMediaId() {
        return logoMediaId;
    }

    public void setLogoMediaId(Long logoMediaId) {
        this.logoMediaId = logoMediaId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
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
}
