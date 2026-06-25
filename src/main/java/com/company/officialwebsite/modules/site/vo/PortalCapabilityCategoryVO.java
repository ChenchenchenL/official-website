package com.company.officialwebsite.modules.site.vo;

import java.util.List;

/**
 * PortalCapabilityCategoryVO：门户前台底座分类视图对象。
 */
public class PortalCapabilityCategoryVO {

    private Long id;
    private String name;
    private List<PortalCapabilityItemVO> items;

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

    public List<PortalCapabilityItemVO> getItems() {
        return items;
    }

    public void setItems(List<PortalCapabilityItemVO> items) {
        this.items = items;
    }
}
