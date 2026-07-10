package com.company.officialwebsite.modules.lead.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.common.utils.DataMaskUtils;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.modules.lead.dto.LeadCreateRequestDTO;
import com.company.officialwebsite.modules.lead.dto.LeadExportRequestDTO;
import com.company.officialwebsite.modules.lead.dto.LeadQueryRequestDTO;
import com.company.officialwebsite.modules.lead.dto.LeadStatusUpdateRequestDTO;
import com.company.officialwebsite.modules.lead.entity.LeadEntity;
import com.company.officialwebsite.modules.lead.enums.LeadExportModeEnum;
import com.company.officialwebsite.modules.lead.enums.LeadStatusEnum;
import com.company.officialwebsite.modules.lead.mapper.LeadMapper;
import com.company.officialwebsite.modules.lead.service.LeadModuleConstants;
import com.company.officialwebsite.modules.lead.service.LeadService;
import com.company.officialwebsite.modules.lead.support.LeadRateLimiter;
import com.company.officialwebsite.modules.lead.vo.AdminLeadDetailVO;
import com.company.officialwebsite.modules.lead.vo.AdminLeadVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LeadServiceImpl：实现前台线索提交、后台分页/详情/状态流转/Excel 导出的核心业务逻辑。
 */
@Service
public class LeadServiceImpl implements LeadService {

    private static final Logger log = LoggerFactory.getLogger(LeadServiceImpl.class);

    private static final String MSG_INVALID_STATUS = "线索状态值不在合法枚举范围内";
    private static final String MSG_NAME_REQUIRED = "姓名不能为空";
    private static final String MSG_NAME_TOO_LONG = "姓名长度超过限制";
    private static final String MSG_COMPANY_REQUIRED = "公司不能为空";
    private static final String MSG_COMPANY_TOO_LONG = "公司长度超过限制";
    private static final String MSG_CONTENT_TOO_LONG = "内容长度超过限制";
    private static final String MSG_TIME_RANGE_INVALID = "提交时间起始不能晚于结束";
    private static final String MSG_EMAIL_REQUIRED = "邮箱不能为空";
    private static final String MSG_EMAIL_INVALID = "邮箱格式不合法";
    private static final String MSG_PHONE_INVALID = "电话包含非法字符";
    private static final String MSG_PHONE_NO_DIGIT = "电话必须至少包含一个数字";
    private static final String MSG_EXPORT_MODE_INVALID = "导出模式不合法";
    private static final String MSG_FILTER_REQUIRED = "筛选导出必须至少包含一个有效筛选条件";
    private static final String MSG_SELECTED_EMPTY = "选中导出时 selectedIds 不能为空";
    private static final String MSG_SELECTED_TOO_MANY = "选中导出 ID 数量超过上限";
    private static final String MSG_EXPORT_TOO_MANY = "导出数据行数超过上限";

    private static final Pattern PHONE_ALLOWED_CHARS = Pattern.compile("^[0-9 +\\-()/#]*$");
    private static final Pattern PHONE_CONTAINS_DIGIT = Pattern.compile("\\d");

    private static final String[] EXPORT_HEADERS = {
            "ID", "姓名", "公司", "邮箱", "电话", "需求描述", "状态", "提交时间", "状态更新时间"
    };

    private final LeadMapper leadMapper;
    private final AuditLogService auditLogService;
    private final LeadRateLimiter leadRateLimiter;

    public LeadServiceImpl(LeadMapper leadMapper, AuditLogService auditLogService, LeadRateLimiter leadRateLimiter) {
        this.leadMapper = leadMapper;
        this.auditLogService = auditLogService;
        this.leadRateLimiter = leadRateLimiter;
    }

    @Override
    @Transactional
    public void createLead(LeadCreateRequestDTO requestDTO, String clientIp, String userAgent) {
        if (!leadRateLimiter.tryAcquire(clientIp)) {
            throw new BusinessException(ErrorCode.LEAD_SUBMIT_RATE_LIMITED);
        }

        String normalizedName = normalizeRequired("姓名", requestDTO.getName(), 64);
        String normalizedCompany = normalizeRequired("公司", requestDTO.getCompany(), 128);
        String normalizedEmail = normalizeEmail(requestDTO.getEmail());
        String normalizedPhone = normalizePhone(requestDTO.getPhone());
        String normalizedDescription = normalizeOptional(requestDTO.getDemandDescription(), 1000);

        LeadEntity entity = new LeadEntity();
        entity.setName(normalizedName);
        entity.setCompany(normalizedCompany);
        entity.setEmail(normalizedEmail);
        entity.setPhone(normalizedPhone);
        entity.setDemandDescription(normalizedDescription);
        entity.setStatus(LeadStatusEnum.UNHANDLED.getCode());
        entity.setSubmitIp(sanitizeIp(clientIp));
        entity.setUserAgent(truncate(userAgent, 255));
        entity.setCreatedBy(LeadModuleConstants.ANONYMOUS_USER_ID);
        entity.setUpdatedBy(LeadModuleConstants.ANONYMOUS_USER_ID);

        leadMapper.insert(entity);
        log.info("create lead success id={} name={} company={}", entity.getId(), normalizedName, normalizedCompany);
        recordAudit(LeadModuleConstants.ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<AdminLeadVO> getAdminLeadPage(LeadQueryRequestDTO requestDTO) {
        int normalizedPageNo = requestDTO.getPageNo() == null || requestDTO.getPageNo() <= 0
                ? 1 : requestDTO.getPageNo();
        int normalizedPageSize = requestDTO.getPageSize() == null || requestDTO.getPageSize() <= 0
                ? 20 : Math.min(requestDTO.getPageSize(), 100);

        validateTimeRange(requestDTO.getSubmitAtStart(), requestDTO.getSubmitAtEnd());
        if (requestDTO.getStatus() != null && !LeadStatusEnum.isValid(requestDTO.getStatus())) {
            throw new BusinessException(ErrorCode.LEAD_STATUS_INVALID, MSG_INVALID_STATUS);
        }

        Page<LeadEntity> page = leadMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                buildQueryWrapper(requestDTO));

        List<AdminLeadVO> list = page.getRecords().stream()
                .map(this::toListVO)
                .toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional()
    public AdminLeadDetailVO getAdminLeadDetail(Long id) {
        LeadEntity entity = requireLead(id);
        log.info("view lead detail id={}", id);
        recordAudit(LeadModuleConstants.ACTION_VIEW_DETAIL, id, null, toSnapshot(entity));
        return toDetailVO(entity);
    }

    @Override
    @Transactional
    public void updateLeadStatus(Long id, LeadStatusUpdateRequestDTO requestDTO, Long operatorId) {
        if (!LeadStatusEnum.isValid(requestDTO.getStatus())) {
            throw new BusinessException(ErrorCode.LEAD_STATUS_INVALID, MSG_INVALID_STATUS);
        }
        LeadEntity entity = requireLead(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());

        Map<String, Object> before = toSnapshot(entity);

        if (entity.getStatus().equals(requestDTO.getStatus())) {
            log.info("update lead status idempotent id={} status={}", id, requestDTO.getStatus());
            return;
        }

        entity.setStatus(requestDTO.getStatus());
        entity.setStatusUpdatedAt(LocalDateTime.now());
        entity.setStatusUpdatedBy(operatorId);
        entity.setUpdatedBy(operatorId);
        ConcurrencyHelper.tryUpdate(leadMapper, entity);

        log.info("update lead status success id={} previousStatus={} currentStatus={}",
                id, before.get("status"), requestDTO.getStatus());
        recordAudit(LeadModuleConstants.ACTION_UPDATE_STATUS, id, before, toSnapshot(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public void exportLeads(LeadExportRequestDTO requestDTO, HttpServletResponse response, Long operatorId) {
        LeadExportModeEnum exportMode = requestDTO.getExportMode();
        if (exportMode == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EXPORT_MODE_INVALID);
        }

        List<LeadEntity> records = switch (exportMode) {
            case FILTERED -> loadFilteredRecords(requestDTO);
            case SELECTED -> loadSelectedRecords(requestDTO);
            default -> throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EXPORT_MODE_INVALID);
        };

        if (records.size() > LeadModuleConstants.EXPORT_MAX_ROWS) {
            throw new BusinessException(ErrorCode.LEAD_EXPORT_TOO_LARGE, MSG_EXPORT_TOO_MANY);
        }

        writeExcel(response, records);
        log.info("export leads success mode={} count={}", exportMode, records.size());
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("exportMode", exportMode.name());
        snapshot.put("recordCount", records.size());
        if (exportMode == LeadExportModeEnum.FILTERED) {
            snapshot.put("submitAtStart", requestDTO.getSubmitAtStart());
            snapshot.put("submitAtEnd", requestDTO.getSubmitAtEnd());
            snapshot.put("status", requestDTO.getStatus());
        } else {
            snapshot.put("selectedIds", requestDTO.getSelectedIds());
        }
        recordAudit(LeadModuleConstants.ACTION_EXPORT, 0L, null, snapshot);
    }

    private List<LeadEntity> loadFilteredRecords(LeadExportRequestDTO requestDTO) {
        if (requestDTO.getSubmitAtStart() == null
                && requestDTO.getSubmitAtEnd() == null
                && requestDTO.getStatus() == null) {
            log.warn("export filtered leads rejected because all filters are empty");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_FILTER_REQUIRED);
        }
        validateTimeRange(requestDTO.getSubmitAtStart(), requestDTO.getSubmitAtEnd());
        if (requestDTO.getStatus() != null && !LeadStatusEnum.isValid(requestDTO.getStatus())) {
            log.warn("export filtered leads rejected because status is invalid status={}", requestDTO.getStatus());
            throw new BusinessException(ErrorCode.LEAD_STATUS_INVALID, MSG_INVALID_STATUS);
        }
        LeadQueryRequestDTO queryDTO = new LeadQueryRequestDTO();
        queryDTO.setSubmitAtStart(requestDTO.getSubmitAtStart());
        queryDTO.setSubmitAtEnd(requestDTO.getSubmitAtEnd());
        queryDTO.setStatus(requestDTO.getStatus());
        return leadMapper.selectList(buildQueryWrapper(queryDTO));
    }

    private List<LeadEntity> loadSelectedRecords(LeadExportRequestDTO requestDTO) {
        List<Long> selectedIds = requestDTO.getSelectedIds();
        if (selectedIds == null || selectedIds.isEmpty()) {
            log.warn("export selected leads rejected because selectedIds is empty");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_SELECTED_EMPTY);
        }
        if (selectedIds.size() > LeadModuleConstants.EXPORT_MAX_SELECTED_IDS) {
            log.warn("export selected leads rejected because selectedIds exceed limit size={}", selectedIds.size());
            throw new BusinessException(ErrorCode.LEAD_EXPORT_TOO_LARGE, MSG_SELECTED_TOO_MANY);
        }
        return leadMapper.selectList(
                new LambdaQueryWrapper<LeadEntity>()
                        .eq(LeadEntity::getDeletedMarker, 0L)
                        .in(LeadEntity::getId, selectedIds)
                        .orderByDesc(LeadEntity::getCreatedAt)
                        .orderByDesc(LeadEntity::getId));
    }

    private LambdaQueryWrapper<LeadEntity> buildQueryWrapper(LeadQueryRequestDTO requestDTO) {
        LambdaQueryWrapper<LeadEntity> wrapper = new LambdaQueryWrapper<LeadEntity>()
                .eq(LeadEntity::getDeletedMarker, 0L);
        if (requestDTO.getSubmitAtStart() != null) {
            wrapper.ge(LeadEntity::getCreatedAt, requestDTO.getSubmitAtStart());
        }
        if (requestDTO.getSubmitAtEnd() != null) {
            wrapper.le(LeadEntity::getCreatedAt, requestDTO.getSubmitAtEnd());
        }
        if (requestDTO.getStatus() != null) {
            wrapper.eq(LeadEntity::getStatus, requestDTO.getStatus());
        }
        return wrapper
                .orderByDesc(LeadEntity::getCreatedAt)
                .orderByDesc(LeadEntity::getId);
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && start.isAfter(end)) {
            log.warn("lead query time range invalid start={} end={}", start, end);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_TIME_RANGE_INVALID);
        }
    }

    private LeadEntity requireLead(Long id) {
        LeadEntity entity = leadMapper.selectOne(
                new LambdaQueryWrapper<LeadEntity>()
                        .eq(LeadEntity::getId, id)
                        .eq(LeadEntity::getDeletedMarker, 0L)
                        .last("limit 1"));
        if (entity == null) {
            log.warn("lead record not found id={}", id);
            throw new BusinessException(ErrorCode.LEAD_RECORD_NOT_FOUND);
        }
        return entity;
    }

    private String normalizeRequired(String fieldName, String value, int maxLength) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            log.warn("lead field is blank fieldName={}", fieldName);
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, requiredMessage(fieldName));
        }
        if (normalized.length() > maxLength) {
            log.warn("lead field exceeds max length fieldName={} maxLength={} actualLength={}",
                    fieldName, maxLength, normalized.length());
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, tooLongMessage(fieldName));
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        String normalized = StringFieldUtils.trimToNull(value);
        if (normalized == null) {
            return null;
        }
        if (normalized.length() > maxLength) {
            log.warn("lead optional content exceeds max length maxLength={} actualLength={}",
                    maxLength, normalized.length());
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_CONTENT_TOO_LONG);
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = StringFieldUtils.trimToNull(email);
        if (normalized == null) {
            log.warn("lead email is blank");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMAIL_REQUIRED);
        }
        if (!isValidEmail(normalized)) {
            log.warn("lead email is invalid");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_EMAIL_INVALID);
        }
        return normalized;
    }

    private boolean isValidEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 0 || atIndex >= email.length() - 1) {
            return false;
        }
        if (email.indexOf('@', atIndex + 1) >= 0) {
            return false;
        }
        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex + 1);
        if (localPart.isEmpty() || localPart.length() > 64) {
            return false;
        }
        return domainPart.lastIndexOf('.') > 0 && domainPart.lastIndexOf('.') < domainPart.length() - 1;
    }

    private String normalizePhone(String phone) {
        String normalized = StringFieldUtils.trimToNull(phone);
        if (normalized == null) {
            return null;
        }
        if (!PHONE_ALLOWED_CHARS.matcher(normalized).matches()) {
            log.warn("lead phone contains illegal characters");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_PHONE_INVALID);
        }
        if (!PHONE_CONTAINS_DIGIT.matcher(normalized).find()) {
            log.warn("lead phone does not contain digits");
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, MSG_PHONE_NO_DIGIT);
        }
        return normalized;
    }

    private String sanitizeIp(String ip) {
        String normalized = StringFieldUtils.trimToNull(ip);
        return normalized == null ? "unknown" : normalized;
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private AdminLeadVO toListVO(LeadEntity entity) {
        AdminLeadVO vo = new AdminLeadVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setCompany(StringFieldUtils.defaultString(entity.getCompany()));
        vo.setMaskedEmail(DataMaskUtils.maskEmail(entity.getEmail()));
        vo.setMaskedPhone(DataMaskUtils.maskPhone(entity.getPhone()));
        vo.setDemandDescriptionPreview(
                DataMaskUtils.previewText(entity.getDemandDescription(), LeadModuleConstants.DEMAND_DESCRIPTION_PREVIEW_LENGTH));
        vo.setStatus(entity.getStatus());
        vo.setStatusLabel(LeadStatusEnum.labelOf(entity.getStatus()));
        vo.setSubmittedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private AdminLeadDetailVO toDetailVO(LeadEntity entity) {
        AdminLeadDetailVO vo = new AdminLeadDetailVO();
        vo.setId(entity.getId());
        vo.setName(StringFieldUtils.defaultString(entity.getName()));
        vo.setCompany(StringFieldUtils.defaultString(entity.getCompany()));
        vo.setEmail(StringFieldUtils.defaultString(entity.getEmail()));
        vo.setPhone(StringFieldUtils.defaultString(entity.getPhone()));
        vo.setDemandDescription(StringFieldUtils.defaultString(entity.getDemandDescription()));
        vo.setStatus(entity.getStatus());
        vo.setStatusLabel(LeadStatusEnum.labelOf(entity.getStatus()));
        vo.setSubmitIp(StringFieldUtils.defaultString(entity.getSubmitIp()));
        vo.setSubmittedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        vo.setVersion(entity.getVersion());
        return vo;
    }

    private Map<String, Object> toSnapshot(LeadEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("name", entity.getName());
        snapshot.put("company", entity.getCompany());
        snapshot.put("email", DataMaskUtils.maskEmail(entity.getEmail()));
        snapshot.put("phone", DataMaskUtils.maskPhone(entity.getPhone()));
        snapshot.put("status", entity.getStatus());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void writeExcel(HttpServletResponse response, List<LeadEntity> records) {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(200)) {
            Sheet sheet = workbook.createSheet("线索列表");
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < EXPORT_HEADERS.length; i++) {
                headerRow.createCell(i).setCellValue(EXPORT_HEADERS[i]);
            }
            int rowIndex = 1;
            for (LeadEntity entity : records) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(entity.getId());
                row.createCell(1).setCellValue(StringFieldUtils.defaultString(entity.getName()));
                row.createCell(2).setCellValue(StringFieldUtils.defaultString(entity.getCompany()));
                row.createCell(3).setCellValue(StringFieldUtils.defaultString(entity.getEmail()));
                row.createCell(4).setCellValue(StringFieldUtils.defaultString(entity.getPhone()));
                row.createCell(5).setCellValue(StringFieldUtils.defaultString(entity.getDemandDescription()));
                row.createCell(6).setCellValue(LeadStatusEnum.labelOf(entity.getStatus()));
                row.createCell(7).setCellValue(formatDateTime(entity.getCreatedAt()));
                row.createCell(8).setCellValue(formatDateTime(entity.getStatusUpdatedAt()));
            }
            String fileName = URLEncoder.encode(
                    "leads_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx",
                    StandardCharsets.UTF_8);
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName);
            response.setHeader("Access-Control-Expose-Headers", "Content-Disposition");
            workbook.write(response.getOutputStream());
            response.getOutputStream().flush();
        } catch (IOException ex) {
            log.error("write lead excel failed", ex);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "导出 Excel 失败");
        }
    }

    private String requiredMessage(String fieldName) {
        return switch (fieldName) {
            case "姓名" -> MSG_NAME_REQUIRED;
            case "公司" -> MSG_COMPANY_REQUIRED;
            default -> ErrorCode.COMMON_PARAM_INVALID.getDefaultMessage();
        };
    }

    private String tooLongMessage(String fieldName) {
        return switch (fieldName) {
            case "姓名" -> MSG_NAME_TOO_LONG;
            case "公司" -> MSG_COMPANY_TOO_LONG;
            default -> MSG_CONTENT_TOO_LONG;
        };
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(
                LeadModuleConstants.BIZ_MODULE,
                actionName,
                LeadModuleConstants.TARGET_TYPE,
                targetId,
                before,
                after);
    }
}
