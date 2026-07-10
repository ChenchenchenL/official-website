package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.ConcurrencyHelper;
import com.company.officialwebsite.modules.pagebuilder.converter.PageDefinitionConverter;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionCreateDTO;
import com.company.officialwebsite.modules.pagebuilder.dto.PageDefinitionUpdateDTO;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.PageStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.enums.PageTypeEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.service.PageDefinitionService;
import com.company.officialwebsite.modules.pagebuilder.vo.PageDefinitionVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PageDefinitionServiceImpl: 页面定义生命周期服务实现类。
 */
@Service
public class PageDefinitionServiceImpl implements PageDefinitionService {

    private static final Logger log = LoggerFactory.getLogger(PageDefinitionServiceImpl.class);

    private static final String BIZ_MODULE = "PAGE_BUILDER";
    private static final String TARGET_TYPE = "PAGE_DEFINITION";
    private static final String ACTION_CREATE = "CREATE_PAGE";
    private static final String ACTION_UPDATE = "UPDATE_PAGE";
    private static final String ACTION_DELETE = "DELETE_PAGE";

    private final PageDefinitionMapper pageDefinitionMapper;
    private final PageDraftMapper pageDraftMapper;
    private final PageDefinitionConverter pageDefinitionConverter;
    private final AuditLogService auditLogService;

    public PageDefinitionServiceImpl(
            PageDefinitionMapper pageDefinitionMapper,
            PageDraftMapper pageDraftMapper,
            PageDefinitionConverter pageDefinitionConverter,
            AuditLogService auditLogService) {
        this.pageDefinitionMapper = pageDefinitionMapper;
        this.pageDraftMapper = pageDraftMapper;
        this.pageDefinitionConverter = pageDefinitionConverter;
        this.auditLogService = auditLogService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PageDefinitionVO> getAdminPageList() {
        List<PageDefinitionEntity> entities = pageDefinitionMapper.selectList(
                new LambdaQueryWrapper<PageDefinitionEntity>()
                        .orderByAsc(PageDefinitionEntity::getSortOrder)
                        .orderByAsc(PageDefinitionEntity::getId)
        );
        return pageDefinitionConverter.toVOList(entities);
    }

    @Override
    @Transactional(readOnly = true)
    public PageDefinitionVO getPageDetail(Long id) {
        PageDefinitionEntity entity = requireActivePage(id);
        return pageDefinitionConverter.toVO(entity);
    }

    @Override
    @Transactional
    public PageDefinitionVO createPage(PageDefinitionCreateDTO dto) {
        // 1. 校验 pageType 合法性
        try {
            PageTypeEnum.valueOf(dto.getPageType().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid pageType: {}", dto.getPageType());
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "页面类型不合法");
        }

        // 2. 校验 pageKey 与 routePath 在活跃数据中是否已存在
        checkDuplicatePageKey(dto.getPageKey(), null);
        checkDuplicateRoutePath(dto.getRoutePath(), null);

        PageDefinitionEntity entity = new PageDefinitionEntity();
        entity.setPageKey(dto.getPageKey().trim());
        entity.setName(dto.getName().trim());
        entity.setRoutePath(dto.getRoutePath().trim());
        entity.setPageType(dto.getPageType().toUpperCase());
        entity.setStatus(PageStatusEnum.ENABLED.name());
        entity.setVisible(dto.getVisible());
        entity.setSortOrder(dto.getSortOrder());

        try {
            pageDefinitionMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            log.warn("DuplicateKeyException while inserting page key={}", dto.getPageKey(), e);
            throw new BusinessException(ErrorCode.PAGE_KEY_DUPLICATE);
        }

        // 3. 初始化对应的空白草稿记录
        PageDraftEntity draft = new PageDraftEntity();
        draft.setPageId(entity.getId());
        draft.setSchemaJson(null);
        draft.setSchemaHash(null);
        draft.setEditorSessionRemark("初始化空白页面草稿");
        pageDraftMapper.insert(draft);

        // 4. 记录审计日志
        Map<String, Object> afterSnapshot = toSnapshot(entity);
        auditLogService.recordGenericOperation(
                BIZ_MODULE, ACTION_CREATE, TARGET_TYPE, entity.getId(), null, afterSnapshot
        );

        log.info("Successfully created page: id={}, key={}", entity.getId(), entity.getPageKey());
        return pageDefinitionConverter.toVO(entity);
    }

    @Override
    @Transactional
    public List<PageDefinitionVO> updatePage(Long id, PageDefinitionUpdateDTO dto) {
        PageDefinitionEntity entity = requireActivePage(id);
        
        ConcurrencyHelper.assertVersion(entity.getVersion(), dto.getVersion());

        checkDuplicateRoutePath(dto.getRoutePath(), id);

        Map<String, Object> beforeSnapshot = toSnapshot(entity);

        entity.setName(dto.getName().trim());
        entity.setRoutePath(dto.getRoutePath().trim());
        entity.setVisible(dto.getVisible());
        entity.setSortOrder(dto.getSortOrder());

        ConcurrencyHelper.tryUpdate(pageDefinitionMapper, entity);

        Map<String, Object> afterSnapshot = toSnapshot(entity);
        auditLogService.recordGenericOperation(
                BIZ_MODULE, ACTION_UPDATE, TARGET_TYPE, entity.getId(), beforeSnapshot, afterSnapshot
        );

        log.info("Successfully updated page: id={}", id);
        return getAdminPageList();
    }

    @Override
    @Transactional
    public List<PageDefinitionVO> deletePage(Long id, Integer version) {
        PageDefinitionEntity entity = requireActivePage(id);

        ConcurrencyHelper.assertVersion(entity.getVersion(), version);

        Map<String, Object> beforeSnapshot = toSnapshot(entity);

        // 1. 逻辑删除页面定义
        pageDefinitionMapper.deleteById(entity);

        // 2. 级联逻辑删除草稿
        PageDraftEntity draft = pageDraftMapper.selectOne(
                new LambdaQueryWrapper<PageDraftEntity>().eq(PageDraftEntity::getPageId, id)
        );
        if (draft != null) {
            pageDraftMapper.deleteById(draft);
        }

        // 3. 记录审计日志
        auditLogService.recordGenericOperation(
                BIZ_MODULE, ACTION_DELETE, TARGET_TYPE, id, beforeSnapshot, null
        );

        log.info("Successfully deleted page and draft: id={}", id);
        return getAdminPageList();
    }

    private PageDefinitionEntity requireActivePage(Long id) {
        PageDefinitionEntity entity = pageDefinitionMapper.selectById(id);
        if (entity == null) {
            log.warn("Page not found or deleted: id={}", id);
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND);
        }
        return entity;
    }

    private void checkDuplicatePageKey(String pageKey, Long excludeId) {
        LambdaQueryWrapper<PageDefinitionEntity> query = new LambdaQueryWrapper<PageDefinitionEntity>()
                .eq(PageDefinitionEntity::getPageKey, pageKey.trim());
        if (excludeId != null) {
            query.ne(PageDefinitionEntity::getId, excludeId);
        }
        if (pageDefinitionMapper.selectCount(query) > 0) {
            log.warn("Duplicate page key: {}", pageKey);
            throw new BusinessException(ErrorCode.PAGE_KEY_DUPLICATE);
        }
    }

    private void checkDuplicateRoutePath(String routePath, Long excludeId) {
        LambdaQueryWrapper<PageDefinitionEntity> query = new LambdaQueryWrapper<PageDefinitionEntity>()
                .eq(PageDefinitionEntity::getRoutePath, routePath.trim());
        if (excludeId != null) {
            query.ne(PageDefinitionEntity::getId, excludeId);
        }
        if (pageDefinitionMapper.selectCount(query) > 0) {
            log.warn("Duplicate route path: {}", routePath);
            throw new BusinessException(ErrorCode.PAGE_ROUTE_DUPLICATE);
        }
    }

    private Map<String, Object> toSnapshot(PageDefinitionEntity entity) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("pageKey", entity.getPageKey());
        snapshot.put("name", entity.getName());
        snapshot.put("routePath", entity.getRoutePath());
        snapshot.put("pageType", entity.getPageType());
        snapshot.put("status", entity.getStatus());
        snapshot.put("visible", entity.getVisible());
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }
}
