package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.response.PageResult;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.modules.site.dto.PageSectionRequestDTO;
import com.company.officialwebsite.modules.site.dto.PageSectionSortItemDTO;
import com.company.officialwebsite.modules.site.dto.PageSectionStatusDTO;
import com.company.officialwebsite.modules.site.dto.PageSectionVisibilityDTO;
import com.company.officialwebsite.modules.site.entity.PageSectionEntity;
import com.company.officialwebsite.modules.site.mapper.PageSectionMapper;
import com.company.officialwebsite.modules.site.service.PageSectionService;
import com.company.officialwebsite.modules.site.vo.PageSectionVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class PageSectionServiceImpl implements PageSectionService {

    private static final Logger log = LoggerFactory.getLogger(PageSectionServiceImpl.class);

    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "PAGE_SECTION";
    private static final String ACTION_CREATE = "CREATE_PAGE_SECTION";
    private static final String ACTION_UPDATE = "UPDATE_PAGE_SECTION";
    private static final String ACTION_DELETE = "DELETE_PAGE_SECTION";
    private static final String ACTION_VISIBILITY = "UPDATE_PAGE_SECTION_VISIBILITY";
    private static final String ACTION_STATUS = "UPDATE_PAGE_SECTION_STATUS";
    private static final String ACTION_REORDER = "REORDER_PAGE_SECTION";
    private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_PUBLISHED = "PUBLISHED";
    private static final String STATUS_OFFLINE = "OFFLINE";

    private final PageSectionMapper pageSectionMapper;
    private final AuditLogService auditLogService;

    public PageSectionServiceImpl(
            PageSectionMapper pageSectionMapper,
            AuditLogService auditLogService) {
        this.pageSectionMapper = pageSectionMapper;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<PageSectionVO> getAdminSections(String pageCode, Integer pageNo, Integer pageSize) {
        int normalizedPageNo = pageNo == null || pageNo <= 0 ? 1 : pageNo;
        int normalizedPageSize = pageSize == null || pageSize <= 0 ? 20 : Math.min(pageSize, 100);
        LambdaQueryWrapper<PageSectionEntity> wrapper = new LambdaQueryWrapper<PageSectionEntity>()
                .eq(PageSectionEntity::getDeletedMarker, 0L);
        if (StringUtils.hasText(pageCode)) {
            wrapper.eq(PageSectionEntity::getPageCode, normalizeCode(pageCode));
        }
        Page<PageSectionEntity> page = pageSectionMapper.selectPage(
                new Page<>(normalizedPageNo, normalizedPageSize),
                wrapper.orderByAsc(PageSectionEntity::getSortOrder).orderByAsc(PageSectionEntity::getId));
        List<PageSectionVO> list = page.getRecords().stream().map(this::toVO).toList();
        return PageResult.of(list, page.getTotal(), normalizedPageNo, normalizedPageSize);
    }

    @Override
    @Transactional(readOnly = true)
    public PageSectionVO getAdminSection(Long id) {
        return toVO(requireActiveSection(id));
    }

    @Override
    @Transactional
    public Long createSection(PageSectionRequestDTO requestDTO) {
        PageSectionEntity entity = new PageSectionEntity();
        applyRequest(entity, requestDTO);
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(nextSortOrder(entity.getPageCode()));
        }
        try {
            pageSectionMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, "同一页面下区块编码已存在");
        }

        log.info("create page section success id={} pageCode={} sectionCode={}",
                entity.getId(), entity.getPageCode(), entity.getSectionCode());
        recordAudit(ACTION_CREATE, entity.getId(), null, toSnapshot(entity));
        return entity.getId();
    }

    @Override
    @Transactional
    public PageSectionVO updateSection(Long id, PageSectionRequestDTO requestDTO) {
        PageSectionEntity entity = requireActiveSection(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        applyRequest(entity, requestDTO);
        try {
            ConcurrencyHelper.tryUpdate(pageSectionMapper, entity);
        } catch (DuplicateKeyException ex) {
            throw new BusinessException(ErrorCode.COMMON_DUPLICATE_DATA, "同一页面下区块编码已存在");
        }

        log.info("update page section success id={} version={}", entity.getId(), entity.getVersion());
        recordAudit(ACTION_UPDATE, entity.getId(), before, toSnapshot(entity));
        return toVO(entity);
    }

    @Override
    @Transactional
    public void deleteSection(Long id, Integer version) {
        PageSectionEntity entity = requireActiveSection(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), version);
        Map<String, Object> before = toSnapshot(entity);
        int deleted = pageSectionMapper.delete(new LambdaUpdateWrapper<PageSectionEntity>()
                .eq(PageSectionEntity::getId, entity.getId())
                .eq(PageSectionEntity::getVersion, version));
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, ConcurrencyHelper.STATE_CONFLICT_MSG);
        }
        recordAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
    }

    @Override
    @Transactional
    public PageSectionVO updateVisibility(Long id, PageSectionVisibilityDTO requestDTO) {
        PageSectionEntity entity = requireActiveSection(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        entity.setVisible(requestDTO.getVisible());
        ConcurrencyHelper.tryUpdate(pageSectionMapper, entity);
        recordAudit(ACTION_VISIBILITY, entity.getId(), before, toSnapshot(entity));
        return toVO(entity);
    }

    @Override
    @Transactional
    public PageSectionVO updateStatus(Long id, PageSectionStatusDTO requestDTO) {
        PageSectionEntity entity = requireActiveSection(id);
        ConcurrencyHelper.assertVersion(entity.getVersion(), requestDTO.getVersion());
        Map<String, Object> before = toSnapshot(entity);
        entity.setStatus(normalizeContentStatus(requestDTO.getStatus(), null));
        ConcurrencyHelper.tryUpdate(pageSectionMapper, entity);
        recordAudit(ACTION_STATUS, entity.getId(), before, toSnapshot(entity));
        return toVO(entity);
    }

    @Override
    @Transactional
    public void batchSort(List<PageSectionSortItemDTO> sortItems) {
        if (sortItems == null || sortItems.isEmpty()) {
            return;
        }
        List<Long> ids = sortItems.stream().map(PageSectionSortItemDTO::getId).toList();
        Set<Long> deduplicatedIds = new LinkedHashSet<>(ids);
        if (deduplicatedIds.size() != ids.size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复区块");
        }
        List<PageSectionEntity> sections = pageSectionMapper.selectList(new LambdaQueryWrapper<PageSectionEntity>()
                .in(PageSectionEntity::getId, deduplicatedIds)
                .eq(PageSectionEntity::getDeletedMarker, 0L));
        if (sections.size() != deduplicatedIds.size()) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序区块不存在或已删除，请刷新后重试");
        }
        Map<Long, PageSectionEntity> entityMap = new HashMap<>();
        for (PageSectionEntity section : sections) {
            entityMap.put(section.getId(), section);
        }
        List<Map<String, Object>> before = sections.stream()
                .sorted(Comparator.comparing(PageSectionEntity::getSortOrder).thenComparing(PageSectionEntity::getId))
                .map(this::toSnapshot)
                .toList();
        for (PageSectionSortItemDTO item : sortItems) {
            PageSectionEntity entity = entityMap.get(item.getId());
            entity.setSortOrder(item.getSortOrder());
            ConcurrencyHelper.tryUpdate(pageSectionMapper, entity);
        }
        List<Map<String, Object>> after = sortItems.stream()
                .map(item -> entityMap.get(item.getId()))
                .map(this::toSnapshot)
                .toList();
        recordAudit(ACTION_REORDER, 0L, before, after);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PageSectionVO> getPortalSections(String pageCode) {
        String normalizedPageCode = normalizeCode(pageCode);
        return pageSectionMapper.selectList(new LambdaQueryWrapper<PageSectionEntity>()
                        .eq(PageSectionEntity::getDeletedMarker, 0L)
                        .eq(PageSectionEntity::getPageCode, normalizedPageCode)
                        .eq(PageSectionEntity::getVisible, true)
                        .eq(PageSectionEntity::getStatus, STATUS_PUBLISHED)
                        .orderByAsc(PageSectionEntity::getSortOrder)
                        .orderByAsc(PageSectionEntity::getId))
                .stream()
                .map(this::toVO)
                .toList();
    }

    private void applyRequest(PageSectionEntity entity, PageSectionRequestDTO requestDTO) {
        entity.setPageCode(normalizeCode(requestDTO.getPageCode()));
        entity.setSectionCode(normalizeCode(requestDTO.getSectionCode()));
        entity.setTitle(requestDTO.getTitle().trim());
        entity.setSubtitle(trimToNull(requestDTO.getSubtitle()));
        entity.setDescription(trimToNull(requestDTO.getDescription()));
        entity.setContentJson(trimToNull(requestDTO.getContentJson()));
        if (requestDTO.getSortOrder() != null) {
            entity.setSortOrder(requestDTO.getSortOrder());
        }
        entity.setVisible(Boolean.TRUE.equals(requestDTO.getVisible()));
        entity.setStatus(normalizeContentStatus(requestDTO.getStatus(), STATUS_DRAFT));
    }

    private PageSectionEntity requireActiveSection(Long id) {
        PageSectionEntity entity = pageSectionMapper.selectOne(new LambdaQueryWrapper<PageSectionEntity>()
                .eq(PageSectionEntity::getId, id)
                .eq(PageSectionEntity::getDeletedMarker, 0L));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "页面区块不存在或已删除");
        }
        return entity;
    }

    private int nextSortOrder(String pageCode) {
        PageSectionEntity last = pageSectionMapper.selectOne(new LambdaQueryWrapper<PageSectionEntity>()
                .eq(PageSectionEntity::getDeletedMarker, 0L)
                .eq(PageSectionEntity::getPageCode, pageCode)
                .orderByDesc(PageSectionEntity::getSortOrder)
                .orderByDesc(PageSectionEntity::getId)
                .last("limit 1"));
        int current = last == null || last.getSortOrder() == null ? 0 : last.getSortOrder();
        if (current > Integer.MAX_VALUE - 10) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值已达到上限，请先整理现有区块");
        }
        return current + 10;
    }

    private PageSectionVO toVO(PageSectionEntity entity) {
        PageSectionVO vo = new PageSectionVO();
        vo.setId(entity.getId());
        vo.setPageCode(entity.getPageCode());
        vo.setSectionCode(entity.getSectionCode());
        vo.setTitle(entity.getTitle());
        vo.setSubtitle(entity.getSubtitle());
        vo.setDescription(entity.getDescription());
        vo.setContentJson(entity.getContentJson());
        vo.setSortOrder(entity.getSortOrder());
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setStatus(entity.getStatus());
        vo.setVersion(entity.getVersion());
        vo.setCreatedAt(entity.getCreatedAt());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private String normalizeCode(String code) {
        if (!StringUtils.hasText(code)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "页面编码不能为空");
        }
        return code.trim().toLowerCase();
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String normalizeContentStatus(String status, String defaultStatus) {
        String normalized = status == null || status.isBlank() ? defaultStatus : status.trim().toUpperCase();
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "内容状态不能为空");
        }
        if (!Set.of(STATUS_DRAFT, STATUS_PUBLISHED, STATUS_OFFLINE).contains(normalized)) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "内容状态只能是 DRAFT、PUBLISHED 或 OFFLINE");
        }
        return normalized;
    }

    private Map<String, Object> toSnapshot(PageSectionEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("pageCode", entity.getPageCode());
        snapshot.put("sectionCode", entity.getSectionCode());
        snapshot.put("title", entity.getTitle());
        snapshot.put("subtitle", entity.getSubtitle());
        snapshot.put("description", entity.getDescription());
        snapshot.put("contentJson", entity.getContentJson());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("status", entity.getStatus());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void recordAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
