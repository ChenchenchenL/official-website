package com.company.officialwebsite.modules.site.vo;

import java.util.ArrayList;
import java.util.List;

/**
 * PortalNavigationMenuVO：用于前台返回公开可见的导航菜单树。
 */
public class PortalNavigationMenuVO {

    private Long id;
    private String menuName;
    private String targetType;
    private String routePath;
    private String anchorCode;
    private String externalUrl;
    private Boolean openInNewTab;
    private List<PortalNavigationMenuVO> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public List<PortalNavigationMenuVO> getChildren() {
        return children;
    }

    public void setChildren(List<PortalNavigationMenuVO> children) {
        this.children = children;
    }
}
