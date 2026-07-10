package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.pagebuilder.constants.PageBuilderConstants;
import com.company.officialwebsite.modules.pagebuilder.converter.PageVersionConverter;
import com.company.officialwebsite.modules.pagebuilder.dto.PagePublishDTO;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDefinitionEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDependencyEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageDraftEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PagePublishSnapshotEntity;
import com.company.officialwebsite.modules.pagebuilder.entity.PageVersionEntity;
import com.company.officialwebsite.modules.pagebuilder.enums.PageVersionSourceTypeEnum;
import com.company.officialwebsite.modules.pagebuilder.enums.PublishSnapshotStatusEnum;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDefinitionMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDependencyMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageDraftMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PagePublishSnapshotMapper;
import com.company.officialwebsite.modules.pagebuilder.mapper.PageVersionMapper;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.service.ComponentTemplateService;
import com.company.officialwebsite.modules.pagebuilder.service.PagePublishService;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import com.company.officialwebsite.modules.pagebuilder.vo.PageVersionVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PagePublishServiceImpl: 页面版本快照发布与回滚管理服务实现类。
 */
@Service
public class PagePublishServiceImpl implements PagePublishService {

    private static final Logger log = LoggerFactory.getLogger(PagePublishServiceImpl.class);

    private static final String BIZ_MODULE = "PAGE_BUILDER";
    private static final String TARGET_TYPE = "PAGE_SNAPSHOT";
    private static final String ACTION_PUBLISH = "PUBLISH_PAGE";
    private static final String ACTION_ROLLBACK = "ROLLBACK_PAGE";

    private final PageDefinitionMapper pageDefinitionMapper;
    private final PageDraftMapper pageDraftMapper;
    private final PageVersionMapper pageVersionMapper;
    private final PagePublishSnapshotMapper pagePublishSnapshotMapper;
    private final PageDependencyMapper pageDependencyMapper;
    private final ComponentTemplateService componentTemplateService;
    private final PageVersionConverter pageVersionConverter;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;

    public PagePublishServiceImpl(
            PageDefinitionMapper pageDefinitionMapper,
            PageDraftMapper pageDraftMapper,
            PageVersionMapper pageVersionMapper,
            PagePublishSnapshotMapper pagePublishSnapshotMapper,
            PageDependencyMapper pageDependencyMapper,
            ComponentTemplateService componentTemplateService,
            PageVersionConverter pageVersionConverter,
            AuditLogService auditLogService,
            PortalCacheSupport portalCacheSupport) {
        this.pageDefinitionMapper = pageDefinitionMapper;
        this.pageDraftMapper = pageDraftMapper;
        this.pageVersionMapper = pageVersionMapper;
        this.pagePublishSnapshotMapper = pagePublishSnapshotMapper;
        this.pageDependencyMapper = pageDependencyMapper;
        this.componentTemplateService = componentTemplateService;
        this.pageVersionConverter = pageVersionConverter;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
    }

    @Override
    @Transactional
    public PageVersionVO publishPage(Long pageId, PagePublishDTO dto) {
        PageDefinitionEntity page = requireActivePage(pageId);

        PageDraftEntity draft = pageDraftMapper.selectOne(
                new LambdaQueryWrapper<PageDraftEntity>().eq(PageDraftEntity::getPageId, pageId)
        );
        if (draft == null || draft.getSchemaJson() == null) {
            log.warn("publishPage failed: pageId={} draft or schema is null", pageId);
            throw new BusinessException(ErrorCode.PAGE_DRAFT_NOT_FOUND);
        }

        // 1. 计算版本序号
        int nextVerNo = getNextVersionNo(pageId);

        // 2. 生成版本记录
        PageVersionEntity version = new PageVersionEntity();
        version.setPageId(pageId);
        version.setVersionNo(nextVerNo);
        version.setSourceType(PageVersionSourceTypeEnum.PUBLISH_BASE.name());
        version.setSchemaJson(draft.getSchemaJson());
        version.setSchemaHash(draft.getSchemaHash());
        version.setChangeSummary(dto.getChangeSummary().trim());
        pageVersionMapper.insert(version);

        // 3. 更新之前的 ACTIVE 快照为 SUPERSEDED
        pagePublishSnapshotMapper.update(null,
                new LambdaUpdateWrapper<PagePublishSnapshotEntity>()
                        .eq(PagePublishSnapshotEntity::getPageId, pageId)
                        .eq(PagePublishSnapshotEntity::getPublishStatus, PublishSnapshotStatusEnum.ACTIVE.name())
                        .set(PagePublishSnapshotEntity::getPublishStatus, PublishSnapshotStatusEnum.SUPERSEDED.name())
        );

        // 4. 创建新的 ACTIVE 发布快照
        PagePublishSnapshotEntity snapshot = new PagePublishSnapshotEntity();
        snapshot.setPageId(pageId);
        snapshot.setVersionId(version.getId());
        snapshot.setSnapshotJson(draft.getSchemaJson());
        snapshot.setSnapshotHash(draft.getSchemaHash());
        snapshot.setPublishStatus(PublishSnapshotStatusEnum.ACTIVE.name());
        pagePublishSnapshotMapper.insert(snapshot);

        // 5. 提取并持久化数据依赖关系
        extractAndSaveDependencies(pageId, snapshot.getId(), draft.getSchemaJson());

        // 6. 审计日志
        Map<String, Object> snapshotLog = new LinkedHashMap<>();
        snapshotLog.put("pageId", pageId);
        snapshotLog.put("pageKey", page.getPageKey());
        snapshotLog.put("versionNo", nextVerNo);
        snapshotLog.put("changeSummary", version.getChangeSummary());
        auditLogService.recordGenericOperation(
                BIZ_MODULE, ACTION_PUBLISH, TARGET_TYPE, snapshot.getId(), null, snapshotLog
        );

        // 7. 失效缓存
        invalidateCache(page.getPageKey(), page.getRoutePath());

        log.info("Successfully published page: pageId={}, versionNo={}", pageId, nextVerNo);
        return pageVersionConverter.toVO(version);
    }

    @Override
    @Transactional
    public PageVersionVO rollbackPage(Long pageId, Long versionId) {
        PageDefinitionEntity page = requireActivePage(pageId);

        PageVersionEntity targetVersion = pageVersionMapper.selectById(versionId);
        if (targetVersion == null || !targetVersion.getPageId().equals(pageId)) {
            log.warn("rollbackPage failed: target version not found or pageId mismatch");
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "指定的历史版本不存在");
        }

        // 1. 计算版本序号
        int nextVerNo = getNextVersionNo(pageId);

        // 2. 产生新的回滚历史版本记录
        PageVersionEntity rollbackVersion = new PageVersionEntity();
        rollbackVersion.setPageId(pageId);
        rollbackVersion.setVersionNo(nextVerNo);
        rollbackVersion.setSourceType(PageVersionSourceTypeEnum.ROLLBACK_BASE.name());
        rollbackVersion.setSchemaJson(targetVersion.getSchemaJson());
        rollbackVersion.setSchemaHash(targetVersion.getSchemaHash());
        rollbackVersion.setChangeSummary("回滚至版本 No." + targetVersion.getVersionNo() + " (变更说明: " + targetVersion.getChangeSummary() + ")");
        pageVersionMapper.insert(rollbackVersion);

        // 3. 将原有的 ACTIVE 快照更新为 SUPERSEDED
        pagePublishSnapshotMapper.update(null,
                new LambdaUpdateWrapper<PagePublishSnapshotEntity>()
                        .eq(PagePublishSnapshotEntity::getPageId, pageId)
                        .eq(PagePublishSnapshotEntity::getPublishStatus, PublishSnapshotStatusEnum.ACTIVE.name())
                        .set(PagePublishSnapshotEntity::getPublishStatus, PublishSnapshotStatusEnum.SUPERSEDED.name())
        );

        // 4. 创建新的 ACTIVE 发布快照
        PagePublishSnapshotEntity snapshot = new PagePublishSnapshotEntity();
        snapshot.setPageId(pageId);
        snapshot.setVersionId(rollbackVersion.getId());
        snapshot.setSnapshotJson(targetVersion.getSchemaJson());
        snapshot.setSnapshotHash(targetVersion.getSchemaHash());
        snapshot.setPublishStatus(PublishSnapshotStatusEnum.ACTIVE.name());
        pagePublishSnapshotMapper.insert(snapshot);

        // 5. 同步覆盖草稿配置
        PageDraftEntity draft = pageDraftMapper.selectOne(
                new LambdaQueryWrapper<PageDraftEntity>().eq(PageDraftEntity::getPageId, pageId)
        );
        if (draft != null) {
            draft.setSchemaJson(targetVersion.getSchemaJson());
            draft.setSchemaHash(targetVersion.getSchemaHash());
            draft.setEditorSessionRemark("由于回滚版本同步重置草稿");
            pageDraftMapper.updateById(draft);
        }

        // 6. 重建页面数据依赖关系
        pageDependencyMapper.delete(
                new LambdaQueryWrapper<PageDependencyEntity>().eq(PageDependencyEntity::getPageId, pageId)
        );
        extractAndSaveDependencies(pageId, snapshot.getId(), targetVersion.getSchemaJson());

        // 7. 审计日志
        Map<String, Object> snapshotLog = new LinkedHashMap<>();
        snapshotLog.put("pageId", pageId);
        snapshotLog.put("pageKey", page.getPageKey());
        snapshotLog.put("rollbackToVersionNo", targetVersion.getVersionNo());
        snapshotLog.put("newVersionNo", nextVerNo);
        auditLogService.recordGenericOperation(
                BIZ_MODULE, ACTION_ROLLBACK, TARGET_TYPE, snapshot.getId(), null, snapshotLog
        );

        // 8. 失效缓存
        invalidateCache(page.getPageKey(), page.getRoutePath());

        log.info("Successfully rolled back page: pageId={}, newVersionNo={}", pageId, nextVerNo);
        return pageVersionConverter.toVO(rollbackVersion);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PageVersionVO> listVersions(Long pageId) {
        requireActivePage(pageId);
        List<PageVersionEntity> entities = pageVersionMapper.selectList(
                new LambdaQueryWrapper<PageVersionEntity>()
                        .eq(PageVersionEntity::getPageId, pageId)
                        .orderByDesc(PageVersionEntity::getVersionNo)
        );
        return pageVersionConverter.toVOList(entities);
    }

    private PageDefinitionEntity requireActivePage(Long id) {
        PageDefinitionEntity entity = pageDefinitionMapper.selectById(id);
        if (entity == null) {
            log.warn("Page definition not found or deleted: id={}", id);
            throw new BusinessException(ErrorCode.PAGE_NOT_FOUND);
        }
        return entity;
    }

    private int getNextVersionNo(Long pageId) {
        List<PageVersionEntity> versions = pageVersionMapper.selectList(
                new LambdaQueryWrapper<PageVersionEntity>()
                        .eq(PageVersionEntity::getPageId, pageId)
                        .orderByDesc(PageVersionEntity::getVersionNo)
        );
        if (versions.isEmpty()) {
            return 1;
        }
        return versions.get(0).getVersionNo() + 1;
    }

    private void extractAndSaveDependencies(Long pageId, Long snapshotId, PageSchemaModel schema) {
        if (schema == null || schema.getSections() == null) {
            return;
        }

        Set<String> insertedKeys = new HashSet<>();

        for (SectionModel section : schema.getSections()) {
            if (section.getComponent() == null) {
                continue;
            }
            ComponentTemplateVO template = null;
            try {
                template = componentTemplateService.getTemplateByCode(section.getComponent());
            } catch (Exception e) {
                // Ignore missing templates
            }
            
            // 1. 提取 MEDIA 属性依赖
            if (template != null && template.getSchemaDefinitionJson() != null) {
                Map<String, Object> schemaDef = template.getSchemaDefinitionJson();
                if (schemaDef.containsKey("fields") && schemaDef.get("fields") instanceof List) {
                    List<?> fieldsList = (List<?>) schemaDef.get("fields");
                    for (Object fieldObj : fieldsList) {
                        if (fieldObj instanceof Map) {
                            Map<?, ?> fieldMap = (Map<?, ?>) fieldObj;
                            String fieldKey = (String) fieldMap.get("fieldKey");
                            String type = (String) fieldMap.get("type");
                            if ("MEDIA".equals(type) && section.getProps() != null) {
                                Object val = section.getProps().get(fieldKey);
                                if (val != null) {
                                    String valStr = val.toString().trim();
                                    if (!valStr.isEmpty()) {
                                        saveDependencySafely(pageId, snapshotId, section.getId(), "MEDIA", "media", "MediaAssetEntity", valStr, insertedKeys);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. 提取 ENTITY / AGGREGATE 绑定依赖
            BindingModel binding = section.getBinding();
            if (binding != null && binding.getMode() != null) {
                String mode = binding.getMode().trim().toUpperCase();
                String source = binding.getSource();
                if (("ENTITY".equals(mode) || "AGGREGATE".equals(mode)) && source != null && !source.trim().isEmpty()) {
                    source = source.trim().toLowerCase();
                    String targetModule = determineTargetModule(source);
                    String targetEntityType = determineTargetEntityType(source);

                    Map<String, Object> query = binding.getQuery();
                    if (query != null) {
                        if (query.containsKey("id") && query.get("id") != null) {
                            saveDependencySafely(pageId, snapshotId, section.getId(), "ENTITY", targetModule, targetEntityType, query.get("id").toString().trim(), insertedKeys);
                        } else if (query.containsKey("ids") && query.get("ids") instanceof List) {
                            List<?> ids = (List<?>) query.get("ids");
                            for (Object idObj : ids) {
                                if (idObj != null) {
                                    saveDependencySafely(pageId, snapshotId, section.getId(), "ENTITY", targetModule, targetEntityType, idObj.toString().trim(), insertedKeys);
                                }
                            }
                        } else {
                            saveDependencySafely(pageId, snapshotId, section.getId(), "ENTITY", targetModule, targetEntityType, "ALL", insertedKeys);
                        }
                    } else {
                        saveDependencySafely(pageId, snapshotId, section.getId(), "ENTITY", targetModule, targetEntityType, "ALL", insertedKeys);
                    }
                }
            }
        }
    }

    private void saveDependencySafely(Long pageId, Long snapshotId, String sectionId, String type, String module, String entityType, String entityId, Set<String> insertedKeys) {
        String key = snapshotId + "_" + sectionId + "_" + type + "_" + module + "_" + entityType + "_" + entityId;
        if (insertedKeys.add(key)) {
            PageDependencyEntity dep = new PageDependencyEntity();
            dep.setPageId(pageId);
            dep.setSnapshotId(snapshotId);
            dep.setComponentInstanceId(sectionId);
            dep.setDependencyType(type);
            dep.setTargetModule(module);
            dep.setTargetEntityType(entityType);
            dep.setTargetEntityId(entityId);
            pageDependencyMapper.insert(dep);
        }
    }

    private String determineTargetModule(String source) {
        switch (source) {
            case "product":
            case "industry_solution":
                return "product";
            case "case":
                return "casecenter";
            case "contact_info":
            case "cooperation_direction_tag":
                return "lead";
            default:
                return "site";
        }
    }

    private String determineTargetEntityType(String source) {
        switch (source) {
            case "product":
                return "Product";
            case "industry_solution":
                return "IndustrySolution";
            case "case":
                return "Case";
            case "timeline_event":
                return "TimelineEvent";
            case "value_card":
                return "ValueCard";
            case "promise_tag":
                return "PromiseTag";
            case "cooperation_direction_tag":
                return "CooperationDirectionTag";
            case "navigation_menu":
                return "NavigationMenu";
            case "home_metric_card":
                return "HomeMetricCard";
            case "site_config":
                return "SiteConfig";
            case "contact_info":
                return "ContactInfo";
            default:
                return "Generic";
        }
    }

    private void invalidateCache(String pageKey, String routePath) {
        String pageKeyCache = PageBuilderConstants.PORTAL_PAGE_CACHE_PREFIX + routePath;
        String metaKeyCache = PageBuilderConstants.PORTAL_PAGE_META_CACHE_PREFIX + pageKey;
        portalCacheSupport.invalidate(pageKeyCache, metaKeyCache);
    }
}
