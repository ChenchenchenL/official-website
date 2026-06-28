package com.company.officialwebsite.modules.lead.dto;

import com.company.officialwebsite.modules.lead.enums.LeadExportModeEnum;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * LeadExportRequestDTO：承载后台线索 Excel 导出的请求参数。
 */
public class LeadExportRequestDTO {

    @NotNull(message = "导出模式不能为空")
    private LeadExportModeEnum exportMode;

    private LocalDateTime submitAtStart;
    private LocalDateTime submitAtEnd;
    private Integer status;
    private List<Long> selectedIds;

    public LeadExportModeEnum getExportMode() {
        return exportMode;
    }

    public void setExportMode(LeadExportModeEnum exportMode) {
        this.exportMode = exportMode;
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

    public List<Long> getSelectedIds() {
        return selectedIds;
    }

    public void setSelectedIds(List<Long> selectedIds) {
        this.selectedIds = selectedIds;
    }
}
