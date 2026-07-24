package com.company.officialwebsite.modules.pagebuilder.model;

/**
 * ComponentLayoutModel: 组件/区块级别布局控制配置模型。
 */
public class ComponentLayoutModel {

    private String position;
    private Object x;
    private Object y;
    private Object width;
    private Object height;
    private Integer zIndex;

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Object getX() {
        return x;
    }

    public void setX(Object x) {
        this.x = x;
    }

    public Object getY() {
        return y;
    }

    public void setY(Object y) {
        this.y = y;
    }

    public Object getWidth() {
        return width;
    }

    public void setWidth(Object width) {
        this.width = width;
    }

    public Object getHeight() {
        return height;
    }

    public void setHeight(Object height) {
        this.height = height;
    }

    public Integer getZIndex() {
        return zIndex;
    }

    public void setZIndex(Integer zIndex) {
        this.zIndex = zIndex;
    }
}
