package com.company.officialwebsite.modules.site.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * NavigationMenuCreateRequestDTO：承载后台新增导航菜单项的请求参数。
 */
public class NavigationMenuCreateRequestDTO {

    @NotNull(message = "父级菜单不能为空")
    private Long parentId;

    @NotBlank(message = "菜单名称不能为空")
    @Size(max = 64, message = "菜单名称长度不能超过64个字符")
    private String menuName;

    @NotBlank(message = "跳转类型不能为空")
    private String targetType;

    @Size(max = 255, message = "内部路由长度不能超过255个字符")
    private String routePath;

    @Size(max = 64, message = "页面锚点长度不能超过64个字符")
    private String anchorCode;

    @Size(max = 500, message = "外部链接长度不能超过500个字符")
    private String externalUrl;

    @NotNull(message = "是否新窗口打开不能为空")
    private Boolean openInNewTab;

    @NotNull(message = "显示状态不能为空")
    private Boolean visible;

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getMenuName() {
        return menuName;
    }

    public void setMenuName(String menuName) {
        this.menuName = menuName;
    }

    public String getTargetType() {
        return targetType;
    }

    public void setTargetType(String targetType) {
        this.targetType = targetType;
    }

    public String getRoutePath() {
        return routePath;
    }

    public void setRoutePath(String routePath) {
        this.routePath = routePath;
    }

    public String getAnchorCode() {
        return anchorCode;
    }

    public void setAnchorCode(String anchorCode) {
        this.anchorCode = anchorCode;
    }

    public String getExternalUrl() {
        return externalUrl;
    }

    public void setExternalUrl(String externalUrl) {
        this.externalUrl = externalUrl;
    }

    public Boolean getOpenInNewTab() {
        return openInNewTab;
    }

    public void setOpenInNewTab(Boolean openInNewTab) {
        this.openInNewTab = openInNewTab;
    }

    public Boolean getVisible() {
        return visible;
    }

    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
}
