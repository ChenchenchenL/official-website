package com.company.officialwebsite.modules.lead.dto;

import java.time.LocalDateTime;

/**
 * LeadQueryRequestDTO：承载后台线索分页查询的筛选参数。
 */
public class LeadQueryRequestDTO {

    private Integer pageNo;
    private Integer pageSize;
    private LocalDateTime submitAtStart;
    private LocalDateTime submitAtEnd;
    private Integer status;

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

    public LocalDateTime getSubmitAtStart() {
        return submitAtStart;
    }

    public void setSubmitAtStart(LocalDateTime submitAtStart) {
        this.submitAtStart = submitAtStart;
    }

    public LocalDateTime getSubmitAtEnd() {
        return submitAtEnd;
    }

    public void setSubmitAtEnd(LocalDateTime submitAtEnd) {
        this.submitAtEnd = submitAtEnd;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
