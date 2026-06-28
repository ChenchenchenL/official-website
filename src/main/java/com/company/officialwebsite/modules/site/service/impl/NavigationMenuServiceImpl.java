package com.company.officialwebsite.modules.site.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.officialwebsite.common.config.properties.OfficialProperties;
import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.enums.MenuLevelEnum;
import com.company.officialwebsite.common.enums.MenuTargetTypeEnum;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.common.utils.StringFieldUtils;
import com.company.officialwebsite.infrastructure.cache.PortalCacheSupport;
import com.company.officialwebsite.modules.site.dto.NavigationMenuCreateRequestDTO;
import com.company.officialwebsite.modules.site.dto.NavigationMenuOrderRequestDTO;
import com.company.officialwebsite.modules.site.dto.NavigationMenuUpdateRequestDTO;
import com.company.officialwebsite.modules.site.dto.NavigationMenuVisibilityUpdateRequestDTO;
import com.company.officialwebsite.modules.site.entity.NavigationMenuEntity;
import com.company.officialwebsite.modules.site.mapper.NavigationMenuMapper;
import com.company.officialwebsite.modules.site.service.NavigationMenuService;
import com.company.officialwebsite.modules.site.vo.AdminNavigationMenuVO;
import com.company.officialwebsite.modules.site.vo.PortalNavigationMenuVO;
import com.company.officialwebsite.modules.system.service.AuditLogService;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * NavigationMenuServiceImpl：实现顶部导航菜单的后台维护、审计与前台缓存读取逻辑。
 */
@Service
public class NavigationMenuServiceImpl implements NavigationMenuService {

    private static final Logger log = LoggerFactory.getLogger(NavigationMenuServiceImpl.class);
    private static final String ACTION_CREATE = "CREATE_NAVIGATION_MENU";
    private static final String ACTION_UPDATE = "UPDATE_NAVIGATION_MENU";
    private static final String ACTION_CHANGE_VISIBILITY = "CHANGE_NAVIGATION_VISIBILITY";
    private static final String ACTION_DELETE = "DELETE_NAVIGATION_MENU";
    private static final String ACTION_REORDER = "REORDER_NAVIGATION_MENU";
    private static final String CACHE_SEGMENT = "navigation";
    private static final String BIZ_MODULE = "SITE";
    private static final String TARGET_TYPE = "NAVIGATION_MENU";
    private static final long ROOT_PARENT_ID = 0L;
    private static final byte MAX_MENU_DEPTH = 2;
    private static final int MAX_ANCHOR_LENGTH = 64;
    private static final Pattern ANCHOR_PATTERN =
            Pattern.compile("^[A-Za-z][A-Za-z0-9_-]{0," + (MAX_ANCHOR_LENGTH - 1) + "}$");

    private final NavigationMenuMapper navigationMenuMapper;
    private final AuditLogService auditLogService;
    private final PortalCacheSupport portalCacheSupport;
    private final int sortGap;

    public NavigationMenuServiceImpl(
            NavigationMenuMapper navigationMenuMapper,
            AuditLogService auditLogService,
            OfficialProperties officialProperties,
            PortalCacheSupport portalCacheSupport) {
        this.navigationMenuMapper = navigationMenuMapper;
        this.auditLogService = auditLogService;
        this.portalCacheSupport = portalCacheSupport;
        this.sortGap = officialProperties.getCache().getSortGap();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminNavigationMenuVO> getAdminMenuTree() {
        return buildAdminTree(listActiveMenus());
    }

    @Override
    @Transactional
    public List<AdminNavigationMenuVO> createMenu(NavigationMenuCreateRequestDTO requestDTO) {
        NavigationMenuEntity entity = new NavigationMenuEntity();
        fillForCreate(entity, requestDTO);
        tryInsert(entity);
        log.info("create navigation menu success menuId={} parentId={} menuLevel={} targetType={}",
                entity.getId(), entity.getParentId(), entity.getMenuLevel(), entity.getTargetType());
        recordMenuAudit(ACTION_CREATE, entity.getId(), null, toAdminFlatSnapshot(entity));
        invalidatePortalNavigation();
        return buildAdminTree(listActiveMenus());
    }

    @Override
    @Transactional
    public List<AdminNavigationMenuVO> updateMenu(Long menuId, NavigationMenuUpdateRequestDTO requestDTO) {
        NavigationMenuEntity entity = requireActiveMenu(menuId);
        log.info("update navigation menu request menuId={} version={} targetType={}",
                menuId, requestDTO.getVersion(), requestDTO.getTargetType());
        assertVersion(entity.getVersion(), requestDTO.getVersion());
        if (isRootMenu(entity) && hasVisibleOrHiddenChildren(entity.getId())) {
            MenuTargetTypeEnum targetType = parseTargetType(requestDTO.getTargetType());
            if (targetType != MenuTargetTypeEnum.GROUP) {
                throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "存在子菜单的一级菜单只能配置为分组类型");
            }
        }

        Object before = toAdminFlatSnapshot(entity);
        fillForUpdate(entity, requestDTO);
        tryUpdate(entity);
        log.info("update navigation menu success menuId={} parentId={} targetType={} visible={}",
                entity.getId(), entity.getParentId(), entity.getTargetType(), entity.getVisible());
        recordMenuAudit(ACTION_UPDATE, entity.getId(), before, toAdminFlatSnapshot(entity));
        invalidatePortalNavigation();
        return buildAdminTree(listActiveMenus());
    }

    @Override
    @Transactional
    public List<AdminNavigationMenuVO> updateVisibility(Long menuId, NavigationMenuVisibilityUpdateRequestDTO requestDTO) {
        NavigationMenuEntity entity = requireActiveMenu(menuId);
        log.info("update navigation menu visibility request menuId={} version={} visible={}",
                menuId, requestDTO.getVersion(), requestDTO.getVisible());
        assertVersion(entity.getVersion(), requestDTO.getVersion());
        Object before = toAdminFlatSnapshot(entity);
        entity.setVisible(requestDTO.getVisible());
        tryUpdate(entity);
        log.info("change navigation visibility success menuId={} visible={}", entity.getId(), entity.getVisible());
        recordMenuAudit(ACTION_CHANGE_VISIBILITY, entity.getId(), before, toAdminFlatSnapshot(entity));
        invalidatePortalNavigation();
        return buildAdminTree(listActiveMenus());
    }

    @Override
    @Transactional
    public List<AdminNavigationMenuVO> deleteMenu(Long menuId) {
        NavigationMenuEntity entity = requireActiveMenu(menuId);
        List<NavigationMenuEntity> cascadeChildren = isRootMenu(entity) ? listChildren(entity.getId()) : List.of();
        Map<String, Object> before = new HashMap<>();
        before.put("menu", toAdminFlatSnapshot(entity));
        before.put("cascadeChildIds", cascadeChildren.stream().map(NavigationMenuEntity::getId).toList());

        if (isRootMenu(entity) && !cascadeChildren.isEmpty()) {
            for (NavigationMenuEntity child : cascadeChildren) {
                markHiddenBeforeDelete(child);
            }
        }
        markHiddenBeforeDelete(entity);

        log.info("delete navigation menu success menuId={} cascadeChildrenCount={}", entity.getId(), cascadeChildren.size());
        recordMenuAudit(ACTION_DELETE, entity.getId(), before, Map.of("deleted", true));
        invalidatePortalNavigation();
        return buildAdminTree(listActiveMenus());
    }

    @Override
    @Transactional
    public List<AdminNavigationMenuVO> reorderMenus(NavigationMenuOrderRequestDTO requestDTO) {
        Long parentId = normalizeParentId(requestDTO.getParentId());
        List<NavigationMenuEntity> siblings = listChildren(parentId);
        if (siblings.isEmpty()) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND, "排序目标不存在");
        }

        List<Long> requestedOrder = deduplicateIds(requestDTO.getOrderedMenuIds());
        if (requestedOrder.size() != requestDTO.getOrderedMenuIds().size()) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序列表不能包含重复菜单");
        }
        Set<Long> siblingIds = new LinkedHashSet<>(siblings.stream().map(NavigationMenuEntity::getId).toList());
        if (!new LinkedHashSet<>(requestedOrder).equals(siblingIds)) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序列表必须完整覆盖同级菜单");
        }

        Map<Long, NavigationMenuEntity> entityMap = siblings.stream()
                .collect(HashMap::new, (map, entity) -> map.put(entity.getId(), entity), HashMap::putAll);
        List<Map<String, Object>> before = siblings.stream()
                .sorted(menuComparator())
                .map(this::toAdminFlatSnapshot)
                .toList();
        for (int index = 0; index < requestedOrder.size(); index++) {
            NavigationMenuEntity entity = entityMap.get(requestedOrder.get(index));
            entity.setSortOrder(sortOrderForIndex(index));
            tryUpdate(entity);
        }

        List<Map<String, Object>> after = requestedOrder.stream()
                .map(entityMap::get)
                .map(this::toAdminFlatSnapshot)
                .toList();
        log.info("reorder navigation menus success parentId={} menuCount={}", parentId, requestedOrder.size());
        recordMenuAudit(ACTION_REORDER, parentId, before, after);
        invalidatePortalNavigation();
        return buildAdminTree(listActiveMenus());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalNavigationMenuVO> getPortalMenuTree() {
        String cacheKey = portalCacheSupport.buildKey(CACHE_SEGMENT);
        List<PortalNavigationMenuVO> cached = portalCacheSupport.readListCache(cacheKey, PortalNavigationMenuVO.class, CACHE_SEGMENT);
        if (cached != null) {
            return cached;
        }

        List<PortalNavigationMenuVO> menuTree = buildPortalTree(listVisibleMenus());
        portalCacheSupport.writeCache(cacheKey, menuTree, portalCacheSupport.isEmptyResult(menuTree), CACHE_SEGMENT);
        return menuTree;
    }

    private void fillForCreate(NavigationMenuEntity entity, NavigationMenuCreateRequestDTO requestDTO) {
        Long parentId = normalizeParentId(requestDTO.getParentId());
        MenuTargetTypeEnum targetType = parseTargetType(requestDTO.getTargetType());
        String menuName = normalizeMenuName(requestDTO.getMenuName());
        entity.setParentId(parentId);
        entity.setMenuLevel(resolveMenuLevel(parentId).getCode());
        entity.setMenuName(menuName);
        entity.setVisible(requestDTO.getVisible());
        entity.setSortOrder(nextSortOrder(parentId));
        applyTarget(entity, targetType, requestDTO.getRoutePath(), requestDTO.getAnchorCode(), requestDTO.getExternalUrl(),
                requestDTO.getOpenInNewTab());
        validateParentRules(entity);
        assertSiblingNameUnique(parentId, menuName, null);
    }

    private void fillForUpdate(NavigationMenuEntity entity, NavigationMenuUpdateRequestDTO requestDTO) {
        String menuName = normalizeMenuName(requestDTO.getMenuName());
        entity.setMenuName(menuName);
        entity.setVisible(requestDTO.getVisible());
        applyTarget(entity, parseTargetType(requestDTO.getTargetType()), requestDTO.getRoutePath(), requestDTO.getAnchorCode(),
                requestDTO.getExternalUrl(), requestDTO.getOpenInNewTab());
        validateParentRules(entity);
        assertSiblingNameUnique(entity.getParentId(), menuName, entity.getId());
    }

    private void validateParentRules(NavigationMenuEntity entity) {
        MenuLevelEnum menuLevel = MenuLevelEnum.fromCode(entity.getMenuLevel());
        if (menuLevel == MenuLevelEnum.LEVEL_1) {
            return;
        }
        if (entity.getMenuLevel() > MAX_MENU_DEPTH) {
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_LEVEL_INVALID, "导航菜单最多支持二级");
        }
        NavigationMenuEntity parent = requireActiveRootMenu(entity.getParentId());
        if (parseTargetType(parent.getTargetType()) != MenuTargetTypeEnum.GROUP) {
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_PARENT_INVALID, "存在子菜单的一级菜单必须配置为分组类型");
        }
        if (parseTargetType(entity.getTargetType()) == MenuTargetTypeEnum.GROUP) {
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_LEVEL_INVALID, "二级菜单不能配置为分组类型");
        }
        if (parent.getMenuLevel() != MenuLevelEnum.LEVEL_1.getCode()) {
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_PARENT_INVALID, "二级菜单只能挂在一级菜单下");
        }
    }

    private void applyTarget(
            NavigationMenuEntity entity,
            MenuTargetTypeEnum targetType,
            String routePath,
            String anchorCode,
            String externalUrl,
            Boolean openInNewTab) {
        entity.setTargetType(targetType.name());
        entity.setRoutePath(null);
        entity.setAnchorCode(null);
        entity.setExternalUrl(null);
        entity.setOpenInNewTab(false);

        switch (targetType) {
            case GROUP -> {
                if (!StringFieldUtils.isBlank(routePath) || !StringFieldUtils.isBlank(anchorCode)
                        || !StringFieldUtils.isBlank(externalUrl)) {
                    throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "分组类型不能配置跳转目标");
                }
                if (Boolean.TRUE.equals(openInNewTab)) {
                    throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "分组类型不能配置新窗口打开");
                }
            }
            case INTERNAL_ROUTE -> {
                String normalizedRoute = StringFieldUtils.trimToNull(routePath);
                if (!StringFieldUtils.isBlank(anchorCode) || !StringFieldUtils.isBlank(externalUrl) || normalizedRoute == null) {
                    throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "内部路由必须且只能配置 routePath");
                }
                if (!normalizedRoute.startsWith("/") || normalizedRoute.contains("#") || normalizedRoute.contains("?")
                        || normalizedRoute.contains("://")) {
                    throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "内部路由格式不合法");
                }
                if (Boolean.TRUE.equals(openInNewTab)) {
                    throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "内部路由不允许配置新窗口打开");
                }
                entity.setRoutePath(normalizedRoute);
            }
            case PAGE_ANCHOR -> {
                String normalizedAnchor = trimAnchor(anchorCode);
                if (!StringFieldUtils.isBlank(routePath) || !StringFieldUtils.isBlank(externalUrl) || normalizedAnchor == null) {
                    throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "页面锚点必须且只能配置 anchorCode");
                }
                if (!ANCHOR_PATTERN.matcher(normalizedAnchor).matches()) {
                    throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "页面锚点格式不合法");
                }
                if (Boolean.TRUE.equals(openInNewTab)) {
                    throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "页面锚点不允许配置新窗口打开");
                }
                entity.setAnchorCode(normalizedAnchor);
            }
            case EXTERNAL_LINK -> {
                String normalizedUrl = StringFieldUtils.trimToNull(externalUrl);
                if (!StringFieldUtils.isBlank(routePath) || !StringFieldUtils.isBlank(anchorCode) || normalizedUrl == null) {
                    throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "外部链接必须且只能配置 externalUrl");
                }
                validateExternalUrl(normalizedUrl);
                entity.setExternalUrl(normalizedUrl);
                entity.setOpenInNewTab(Boolean.TRUE.equals(openInNewTab));
            }
            default -> throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID);
        }
    }

    private void validateExternalUrl(String url) {
        try {
            URI uri = new URI(url);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            if (scheme == null || host == null || host.isBlank()) {
                throw new IllegalArgumentException("scheme or host missing");
            }
            String normalizedScheme = scheme.toLowerCase(Locale.ROOT);
            if (!"http".equals(normalizedScheme) && !"https".equals(normalizedScheme)) {
                throw new IllegalArgumentException("unsupported scheme");
            }
        } catch (URISyntaxException | IllegalArgumentException ex) {
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "外部链接格式不合法");
        }
    }

    private MenuLevelEnum resolveMenuLevel(Long parentId) {
        if (ROOT_PARENT_ID == parentId) {
            return MenuLevelEnum.LEVEL_1;
        }
        requireActiveRootMenu(parentId);
        return MenuLevelEnum.LEVEL_2;
    }

    private NavigationMenuEntity requireActiveRootMenu(Long parentId) {
        NavigationMenuEntity parent = requireActiveMenu(parentId);
        if (!isRootMenu(parent)) {
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_PARENT_INVALID, "二级菜单只能挂在一级菜单下");
        }
        return parent;
    }

    private boolean hasVisibleOrHiddenChildren(Long parentId) {
        // 仅以当前事务内活跃子菜单为准，删除中的子菜单会在逻辑删除提交后从结果集中消失。
        return navigationMenuMapper.selectCount(new LambdaQueryWrapper<NavigationMenuEntity>()
                .eq(NavigationMenuEntity::getParentId, parentId)
                .eq(NavigationMenuEntity::getDeletedMarker, 0L)) > 0;
    }

    private void assertSiblingNameUnique(Long parentId, String menuName, Long excludeId) {
        LambdaQueryWrapper<NavigationMenuEntity> queryWrapper = new LambdaQueryWrapper<NavigationMenuEntity>()
                .eq(NavigationMenuEntity::getParentId, parentId)
                .eq(NavigationMenuEntity::getMenuName, menuName)
                .eq(NavigationMenuEntity::getDeletedMarker, 0L);
        if (excludeId != null) {
            queryWrapper.ne(NavigationMenuEntity::getId, excludeId);
        }
        if (navigationMenuMapper.selectCount(queryWrapper) > 0) {
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_NAME_DUPLICATE, "同一父级下菜单名称不能重复");
        }
    }

    private NavigationMenuEntity requireActiveMenu(Long menuId) {
        NavigationMenuEntity entity = navigationMenuMapper.selectOne(new LambdaQueryWrapper<NavigationMenuEntity>()
                .eq(NavigationMenuEntity::getId, menuId)
                .eq(NavigationMenuEntity::getDeletedMarker, 0L)
                .last("limit 1"));
        if (entity == null) {
            throw new BusinessException(ErrorCode.COMMON_RESOURCE_NOT_FOUND);
        }
        return entity;
    }

    private List<NavigationMenuEntity> listActiveMenus() {
        return navigationMenuMapper.selectList(new LambdaQueryWrapper<NavigationMenuEntity>()
                .eq(NavigationMenuEntity::getDeletedMarker, 0L)
                .orderByAsc(NavigationMenuEntity::getSortOrder)
                .orderByAsc(NavigationMenuEntity::getId));
    }

    private List<NavigationMenuEntity> listVisibleMenus() {
        return navigationMenuMapper.selectList(new LambdaQueryWrapper<NavigationMenuEntity>()
                .eq(NavigationMenuEntity::getDeletedMarker, 0L)
                .eq(NavigationMenuEntity::getVisible, true)
                .orderByAsc(NavigationMenuEntity::getSortOrder)
                .orderByAsc(NavigationMenuEntity::getId));
    }

    private List<NavigationMenuEntity> listChildren(Long parentId) {
        return navigationMenuMapper.selectList(new LambdaQueryWrapper<NavigationMenuEntity>()
                .eq(NavigationMenuEntity::getParentId, parentId)
                .eq(NavigationMenuEntity::getDeletedMarker, 0L)
                .orderByAsc(NavigationMenuEntity::getSortOrder)
                .orderByAsc(NavigationMenuEntity::getId));
    }

    private int nextSortOrder(Long parentId) {
        NavigationMenuEntity lastSibling = navigationMenuMapper.selectOne(new LambdaQueryWrapper<NavigationMenuEntity>()
                .eq(NavigationMenuEntity::getParentId, parentId)
                .eq(NavigationMenuEntity::getDeletedMarker, 0L)
                .orderByDesc(NavigationMenuEntity::getSortOrder)
                .orderByDesc(NavigationMenuEntity::getId)
                .last("limit 1"));
        int current = lastSibling == null || lastSibling.getSortOrder() == null ? 0 : lastSibling.getSortOrder();
        if (current > Integer.MAX_VALUE - sortGap) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "同级菜单排序值已达到上限");
        }
        return current + sortGap;
    }

    private List<AdminNavigationMenuVO> buildAdminTree(List<NavigationMenuEntity> entities) {
        Map<Long, List<NavigationMenuEntity>> childrenByParent = groupChildren(entities);
        List<AdminNavigationMenuVO> roots = new ArrayList<>();
        for (NavigationMenuEntity entity : entities) {
            if (!isRootMenu(entity)) {
                continue;
            }
            AdminNavigationMenuVO root = toAdminVO(entity);
            root.setChildren(childrenByParent.getOrDefault(entity.getId(), List.of()).stream()
                    .sorted(menuComparator())
                    .map(this::toAdminVO)
                    .toList());
            roots.add(root);
        }
        roots.sort(Comparator.comparing(AdminNavigationMenuVO::getSortOrder).thenComparing(AdminNavigationMenuVO::getId));
        return roots;
    }

    private List<PortalNavigationMenuVO> buildPortalTree(List<NavigationMenuEntity> entities) {
        Map<Long, List<NavigationMenuEntity>> childrenByParent = groupChildren(entities);
        List<NavigationMenuEntity> sortedRoots = entities.stream()
                .filter(this::isRootMenu)
                .sorted(menuComparator())
                .toList();
        List<PortalNavigationMenuVO> roots = new ArrayList<>();
        for (NavigationMenuEntity entity : sortedRoots) {
            List<PortalNavigationMenuVO> children = childrenByParent.getOrDefault(entity.getId(), List.of()).stream()
                    .sorted(menuComparator())
                    .map(this::toPortalVO)
                    .toList();
            if (parseTargetType(entity.getTargetType()) == MenuTargetTypeEnum.GROUP && children.isEmpty()) {
                continue;
            }
            PortalNavigationMenuVO root = toPortalVO(entity);
            root.setChildren(children);
            roots.add(root);
        }
        return roots;
    }

    private Map<Long, List<NavigationMenuEntity>> groupChildren(List<NavigationMenuEntity> entities) {
        Map<Long, List<NavigationMenuEntity>> childrenByParent = new HashMap<>();
        for (NavigationMenuEntity entity : entities) {
            childrenByParent.computeIfAbsent(entity.getParentId(), ignored -> new ArrayList<>()).add(entity);
        }
        return childrenByParent;
    }

    private AdminNavigationMenuVO toAdminVO(NavigationMenuEntity entity) {
        AdminNavigationMenuVO vo = new AdminNavigationMenuVO();
        vo.setId(entity.getId());
        vo.setParentId(entity.getParentId());
        vo.setMenuLevel(MenuLevelEnum.fromCode(entity.getMenuLevel()).name());
        vo.setMenuName(entity.getMenuName());
        vo.setTargetType(entity.getTargetType());
        vo.setRoutePath(entity.getRoutePath());
        vo.setAnchorCode(entity.getAnchorCode());
        vo.setExternalUrl(entity.getExternalUrl());
        vo.setOpenInNewTab(Boolean.TRUE.equals(entity.getOpenInNewTab()));
        vo.setVisible(Boolean.TRUE.equals(entity.getVisible()));
        vo.setSortOrder(entity.getSortOrder());
        vo.setVersion(entity.getVersion());
        vo.setUpdatedAt(entity.getUpdatedAt());
        return vo;
    }

    private PortalNavigationMenuVO toPortalVO(NavigationMenuEntity entity) {
        PortalNavigationMenuVO vo = new PortalNavigationMenuVO();
        vo.setId(entity.getId());
        vo.setMenuName(entity.getMenuName());
        vo.setTargetType(entity.getTargetType());
        vo.setRoutePath(entity.getRoutePath());
        vo.setAnchorCode(entity.getAnchorCode());
        vo.setExternalUrl(entity.getExternalUrl());
        vo.setOpenInNewTab(Boolean.TRUE.equals(entity.getOpenInNewTab()));
        return vo;
    }

    private Map<String, Object> toAdminFlatSnapshot(NavigationMenuEntity entity) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", entity.getId());
        snapshot.put("parentId", entity.getParentId());
        snapshot.put("menuLevel", MenuLevelEnum.fromCode(entity.getMenuLevel()).name());
        snapshot.put("menuName", entity.getMenuName());
        snapshot.put("targetType", entity.getTargetType());
        snapshot.put("routePath", entity.getRoutePath());
        snapshot.put("anchorCode", entity.getAnchorCode());
        snapshot.put("externalUrl", entity.getExternalUrl());
        snapshot.put("openInNewTab", Boolean.TRUE.equals(entity.getOpenInNewTab()));
        snapshot.put("visible", Boolean.TRUE.equals(entity.getVisible()));
        snapshot.put("sortOrder", entity.getSortOrder());
        snapshot.put("version", entity.getVersion());
        return snapshot;
    }

    private void tryInsert(NavigationMenuEntity entity) {
        try {
            navigationMenuMapper.insert(entity);
        } catch (DuplicateKeyException ex) {
            log.warn("duplicate navigation menu name on insert parentId={} menuName={}", entity.getParentId(), entity.getMenuName(), ex);
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_NAME_DUPLICATE, "同一父级下菜单名称不能重复", ex);
        }
    }

    private void tryUpdate(NavigationMenuEntity entity) {
        try {
            int updated = navigationMenuMapper.updateById(entity);
            if (updated != 1) {
                log.warn("update navigation menu failed due to optimistic lock menuId={} requestVersion={}",
                        entity.getId(), entity.getVersion());
                throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "菜单已被其他操作更新，请刷新后重试");
            }
        } catch (DuplicateKeyException ex) {
            log.warn("duplicate navigation menu name on update parentId={} menuName={} menuId={}",
                    entity.getParentId(), entity.getMenuName(), entity.getId(), ex);
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_NAME_DUPLICATE, "同一父级下菜单名称不能重复", ex);
        }
    }

    /**
     * 通过 MyBatis Plus 逻辑删除当前菜单，依赖 BaseEntity.deletedMarker 的逻辑删除配置完成状态变更。
     */
    private void logicallyDeleteById(NavigationMenuEntity entity) {
        int deleted = navigationMenuMapper.deleteById(entity.getId());
        if (deleted != 1) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "菜单已被其他操作更新，请刷新后重试");
        }
    }

    private void markHiddenBeforeDelete(NavigationMenuEntity entity) {
        entity.setVisible(false);
        tryUpdate(entity);
        logicallyDeleteById(entity);
    }

    private void invalidatePortalNavigation() {
        portalCacheSupport.invalidatePortalKey(CACHE_SEGMENT);
    }

    private void assertVersion(Integer currentVersion, Integer requestVersion) {
        if (requestVersion == null || requestVersion < 0) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "版本号不能为负数");
        }
        if (!Objects.equals(currentVersion, requestVersion)) {
            log.warn("navigation menu stale version menuCurrentVersion={} requestVersion={}", currentVersion, requestVersion);
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "菜单已被其他操作更新，请刷新后重试");
        }
    }

    private List<Long> deduplicateIds(Collection<Long> orderedMenuIds) {
        if (orderedMenuIds == null) {
            return List.of();
        }
        Set<Long> deduplicated = new LinkedHashSet<>();
        for (Long orderedMenuId : orderedMenuIds) {
            if (orderedMenuId == null) {
                throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "排序菜单 ID 不能为空");
            }
            deduplicated.add(orderedMenuId);
        }
        return List.copyOf(deduplicated);
    }

    private Comparator<NavigationMenuEntity> menuComparator() {
        return Comparator
                .comparing(NavigationMenuEntity::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(NavigationMenuEntity::getId, Comparator.nullsLast(Long::compareTo));
    }

    private boolean isRootMenu(NavigationMenuEntity entity) {
        return entity != null && Objects.equals(normalizeParentId(entity.getParentId()), ROOT_PARENT_ID);
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null ? ROOT_PARENT_ID : parentId;
    }

    private String normalizeMenuName(String menuName) {
        String normalized = StringFieldUtils.trimToNull(menuName);
        if (normalized == null) {
            throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, "菜单名称不能为空");
        }
        return normalized;
    }

    private MenuTargetTypeEnum parseTargetType(String targetType) {
        try {
            return MenuTargetTypeEnum.valueOf(StringFieldUtils.trimToNull(targetType));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SITE_NAVIGATION_TARGET_INVALID, "跳转类型不合法");
        }
    }

    private String trimAnchor(String anchorCode) {
        String normalized = StringFieldUtils.trimToNull(anchorCode);
        if (normalized == null) {
            return null;
        }
        return normalized.startsWith("#") ? normalized.substring(1) : normalized;
    }

    private int sortOrderForIndex(int index) {
        try {
            return Math.multiplyExact(Math.addExact(index, 1), sortGap);
        } catch (ArithmeticException ex) {
            throw new BusinessException(ErrorCode.COMMON_STATE_CONFLICT, "排序值超出允许范围");
        }
    }

    private void recordMenuAudit(String actionName, Long targetId, Object before, Object after) {
        auditLogService.recordGenericOperation(BIZ_MODULE, actionName, TARGET_TYPE, targetId, before, after);
    }
}
