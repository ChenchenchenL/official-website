package com.company.officialwebsite.common.response;

import java.util.List;

/**
 * PageResult：统一承载分页列表响应，避免各模块重复定义分页外层结构。
 *
 * @param <T> list 中的元素类型
 */
public class PageResult<T> {

    private List<T> list;
    private Long total;
    private Integer pageNo;
    private Integer pageSize;

    public static <T> PageResult<T> of(List<T> list, long total, int pageNo, int pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        return result;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPageNo() {
        return pageNo;
    }

    public void setPageNo(Integer pageNo) {
        this.pageNo = pageNo;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
