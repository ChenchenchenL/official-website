package com.company.officialwebsite.modules.pagebuilder.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * PageCopyDTO: 页面复制与基于模板建页请求数据传输对象。
 */
public class PageCopyDTO {

    private Long sourcePageId;

    private String sourceTemplateCode;

    @NotBlank(message = "目标页面名称不能为空")
    private String targetName;

    @NotBlank(message = "目标页面路由路径不能为空")
    @Pattern(regexp = "^/[a-zA-Z0-9_\\-/]*$", message = "页面路由路径必须以 / 开头，且仅允许包含字母、数字、短横线、下划线及斜杠")
    private String targetPath;

    @NotBlank(message = "目标页面标识Key不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_\\-]+$", message = "页面 Key 仅允许包含字母、数字、下划线及短横线")
    private String targetPageKey;

    public Long getSourcePageId() {
        return sourcePageId;
    }

    public void setSourcePageId(Long sourcePageId) {
        this.sourcePageId = sourcePageId;
    }

    public String getSourceTemplateCode() {
        return sourceTemplateCode;
    }

    public void setSourceTemplateCode(String sourceTemplateCode) {
        this.sourceTemplateCode = sourceTemplateCode;
    }

    public String getTargetName() {
        return targetName;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }

    public String getTargetPageKey() {
        return targetPageKey;
    }

    public void setTargetPageKey(String targetPageKey) {
        this.targetPageKey = targetPageKey;
    }
}
