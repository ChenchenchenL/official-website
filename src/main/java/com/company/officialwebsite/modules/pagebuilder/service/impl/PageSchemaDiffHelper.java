package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.company.officialwebsite.modules.pagebuilder.enums.SchemaChangeTypeEnum;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.pagebuilder.model.ComponentLayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionResponsiveModel;
import com.company.officialwebsite.modules.pagebuilder.model.SeoModel;
import com.company.officialwebsite.modules.pagebuilder.vo.SchemaDiffItemVO;

import java.util.*;

/**
 * PageSchemaDiffHelper: 页面 Schema 结构化深层比对与安全脱敏工具类。
 */
public class PageSchemaDiffHelper {

    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "token", "password", "secret", "filepath", "abspath", "privatekey", "credential"
    );

    /**
     * 比对旧版 Schema 与新版 Schema，输出安全的脱敏变更明细列表。
     */
    public static List<SchemaDiffItemVO> compareSchemas(PageSchemaModel oldSchema, PageSchemaModel newSchema) {
        List<SchemaDiffItemVO> diffs = new ArrayList<>();

        if (oldSchema == null && newSchema == null) {
            return diffs;
        }

        if (oldSchema == null) {
            // 全新页面：新版所有 Sections 均为 ADDED
            if (newSchema.getSections() != null) {
                for (SectionModel sec : newSchema.getSections()) {
                    diffs.add(new SchemaDiffItemVO(
                            "sections[" + sec.getId() + "]",
                            sec.getId(),
                            sec.getComponent(),
                            SchemaChangeTypeEnum.ADDED,
                            "section",
                            null,
                            maskSensitiveValues(sec.getProps())
                    ));
                }
            }
            return diffs;
        }

        if (newSchema == null) {
            if (oldSchema.getSections() != null) {
                for (SectionModel sec : oldSchema.getSections()) {
                    diffs.add(new SchemaDiffItemVO(
                            "sections[" + sec.getId() + "]",
                            sec.getId(),
                            sec.getComponent(),
                            SchemaChangeTypeEnum.REMOVED,
                            "section",
                            maskSensitiveValues(sec.getProps()),
                            null
                    ));
                }
            }
            return diffs;
        }

        // 1. 比较页面基本信息 (name, page-level layout, seo)
        if (!Objects.equals(oldSchema.getName(), newSchema.getName())) {
            diffs.add(new SchemaDiffItemVO(
                    "name", null, null, SchemaChangeTypeEnum.MODIFIED, "name", oldSchema.getName(), newSchema.getName()
            ));
        }

        comparePageLayout("layout", oldSchema.getLayout(), newSchema.getLayout(), diffs);
        comparePageSeo("seo", oldSchema.getSeo(), newSchema.getSeo(), diffs);

        // 2. 比较 Sections (按 Section ID 建立索引匹配)
        Map<String, SectionModel> oldSectionMap = new LinkedHashMap<>();
        if (oldSchema.getSections() != null) {
            for (SectionModel s : oldSchema.getSections()) {
                if (s.getId() != null) {
                    oldSectionMap.put(s.getId(), s);
                }
            }
        }

        List<SectionModel> newSections = newSchema.getSections() != null ? newSchema.getSections() : Collections.emptyList();
        for (SectionModel newSec : newSections) {
            String id = newSec.getId();
            if (id == null) {
                continue;
            }

            if (!oldSectionMap.containsKey(id)) {
                // ADDED
                diffs.add(new SchemaDiffItemVO(
                        "sections[" + id + "]",
                        id,
                        newSec.getComponent(),
                        SchemaChangeTypeEnum.ADDED,
                        "section",
                        null,
                        maskSensitiveValues(newSec.getProps())
                ));
            } else {
                // MODIFIED: 深入逐字段比对组件差异
                SectionModel oldSec = oldSectionMap.remove(id);
                compareSingleSection(id, oldSec, newSec, diffs);
            }
        }

        // 剩在 oldSectionMap 中的代表被 REMOVED
        for (Map.Entry<String, SectionModel> entry : oldSectionMap.entrySet()) {
            SectionModel oldSec = entry.getValue();
            diffs.add(new SchemaDiffItemVO(
                    "sections[" + oldSec.getId() + "]",
                    oldSec.getId(),
                    oldSec.getComponent(),
                    SchemaChangeTypeEnum.REMOVED,
                    "section",
                    maskSensitiveValues(oldSec.getProps()),
                    null
            ));
        }

        return diffs;
    }

    private static void comparePageLayout(String basePath, LayoutModel oldL, LayoutModel newL, List<SchemaDiffItemVO> diffs) {
        if (oldL == null && newL == null) return;

        String oldType = oldL != null ? oldL.getType() : null;
        String newType = newL != null ? newL.getType() : null;

        if (!Objects.equals(oldType, newType)) {
            diffs.add(new SchemaDiffItemVO(
                    basePath + ".type", null, null, SchemaChangeTypeEnum.MODIFIED, "type", oldType, newType
            ));
        }
    }

    private static void comparePageSeo(String basePath, SeoModel oldSeo, SeoModel newSeo, List<SchemaDiffItemVO> diffs) {
        if (oldSeo == null && newSeo == null) return;

        Map<String, Object> oldMap = new HashMap<>();
        if (oldSeo != null) {
            if (oldSeo.getTitle() != null) oldMap.put("title", oldSeo.getTitle());
            if (oldSeo.getKeywords() != null) oldMap.put("keywords", oldSeo.getKeywords());
            if (oldSeo.getDescription() != null) oldMap.put("description", oldSeo.getDescription());
        }

        Map<String, Object> newMap = new HashMap<>();
        if (newSeo != null) {
            if (newSeo.getTitle() != null) newMap.put("title", newSeo.getTitle());
            if (newSeo.getKeywords() != null) newMap.put("keywords", newSeo.getKeywords());
            if (newSeo.getDescription() != null) newMap.put("description", newSeo.getDescription());
        }

        compareMap(basePath, null, null, oldMap, newMap, diffs);
    }

    private static void compareSingleSection(String sectionId, SectionModel oldSec, SectionModel newSec, List<SchemaDiffItemVO> diffs) {
        String code = newSec.getComponent();

        // Compare props
        compareMap("sections[" + sectionId + "].props", sectionId, code, oldSec.getProps(), newSec.getProps(), diffs);

        // Compare style
        compareMap("sections[" + sectionId + "].style", sectionId, code, oldSec.getStyle(), newSec.getStyle(), diffs);

        // Compare layout
        compareLayout("sections[" + sectionId + "].layout", sectionId, code, oldSec.getLayout(), newSec.getLayout(), diffs);

        // Compare binding (P2-2)
        compareBinding("sections[" + sectionId + "].binding", sectionId, code, oldSec.getBinding(), newSec.getBinding(), diffs);

        // Compare responsive
        compareResponsive("sections[" + sectionId + "].responsive", sectionId, code, oldSec.getResponsive(), newSec.getResponsive(), diffs);
    }

    private static void compareBinding(String basePath, String sectionId, String code, BindingModel oldB, BindingModel newB, List<SchemaDiffItemVO> diffs) {
        if (oldB == null && newB == null) return;

        String oldMode = oldB != null ? oldB.getMode() : null;
        String newMode = newB != null ? newB.getMode() : null;
        if (!Objects.equals(oldMode, newMode)) {
            diffs.add(new SchemaDiffItemVO(
                    basePath + ".mode", sectionId, code, SchemaChangeTypeEnum.MODIFIED, "mode", oldMode, newMode
            ));
        }

        String oldSource = oldB != null ? oldB.getSource() : null;
        String newSource = newB != null ? newB.getSource() : null;
        if (!Objects.equals(oldSource, newSource)) {
            diffs.add(new SchemaDiffItemVO(
                    basePath + ".source", sectionId, code, SchemaChangeTypeEnum.MODIFIED, "source", oldSource, newSource
            ));
        }

        Map<String, Object> oldQuery = oldB != null ? oldB.getQuery() : null;
        Map<String, Object> newQuery = newB != null ? newB.getQuery() : null;
        compareMap(basePath + ".query", sectionId, code, oldQuery, newQuery, diffs);
    }

    private static void compareMap(String basePath, String sectionId, String code, Map<String, Object> oldMap, Map<String, Object> newMap, List<SchemaDiffItemVO> diffs) {
        Map<String, Object> oldM = oldMap != null ? oldMap : Collections.emptyMap();
        Map<String, Object> newM = newMap != null ? newMap : Collections.emptyMap();

        Set<String> allKeys = new HashSet<>(oldM.keySet());
        allKeys.addAll(newM.keySet());

        for (String key : allKeys) {
            Object oldVal = oldM.get(key);
            Object newVal = newM.get(key);

            if (!Objects.equals(oldVal, newVal)) {
                diffs.add(new SchemaDiffItemVO(
                        basePath + "." + key,
                        sectionId,
                        code,
                        SchemaChangeTypeEnum.MODIFIED,
                        key,
                        sanitizeValue(key, oldVal),
                        sanitizeValue(key, newVal)
                ));
            }
        }
    }

    private static void compareLayout(String basePath, String sectionId, String code, ComponentLayoutModel oldLayout, ComponentLayoutModel newLayout, List<SchemaDiffItemVO> diffs) {
        if (oldLayout == null && newLayout == null) {
            return;
        }

        Map<String, Object> oldMap = oldLayout != null ? convertLayoutToMap(oldLayout) : Collections.emptyMap();
        Map<String, Object> newMap = newLayout != null ? convertLayoutToMap(newLayout) : Collections.emptyMap();

        compareMap(basePath, sectionId, code, oldMap, newMap, diffs);
    }

    private static Map<String, Object> convertLayoutToMap(ComponentLayoutModel layout) {
        Map<String, Object> map = new HashMap<>();
        if (layout.getPosition() != null) map.put("position", layout.getPosition());
        if (layout.getX() != null) map.put("x", layout.getX());
        if (layout.getY() != null) map.put("y", layout.getY());
        if (layout.getWidth() != null) map.put("width", layout.getWidth());
        if (layout.getHeight() != null) map.put("height", layout.getHeight());
        if (layout.getZIndex() != null) map.put("zIndex", layout.getZIndex());
        return map;
    }

    private static void compareResponsive(String basePath, String sectionId, String code, Map<String, SectionResponsiveModel> oldResp, Map<String, SectionResponsiveModel> newResp, List<SchemaDiffItemVO> diffs) {
        Map<String, SectionResponsiveModel> oldR = oldResp != null ? oldResp : Collections.emptyMap();
        Map<String, SectionResponsiveModel> newR = newResp != null ? newResp : Collections.emptyMap();

        Set<String> keys = new HashSet<>(oldR.keySet());
        keys.addAll(newR.keySet());

        for (String bp : keys) {
            SectionResponsiveModel oldM = oldR.get(bp);
            SectionResponsiveModel newM = newR.get(bp);

            ComponentLayoutModel oldL = oldM != null ? oldM.getLayout() : null;
            ComponentLayoutModel newL = newM != null ? newM.getLayout() : null;
            compareLayout(basePath + "." + bp + ".layout", sectionId, code, oldL, newL, diffs);

            Map<String, Object> oldS = oldM != null ? oldM.getStyle() : null;
            Map<String, Object> newS = newM != null ? newM.getStyle() : null;
            compareMap(basePath + "." + bp + ".style", sectionId, code, oldS, newS, diffs);
        }
    }

    private static Object sanitizeValue(String key, Object val) {
        if (val == null) {
            return null;
        }
        if (key != null) {
            String keyLower = key.toLowerCase();
            for (String sKey : SENSITIVE_KEYS) {
                if (keyLower.contains(sKey)) {
                    return "******";
                }
            }
        }
        return val;
    }

    private static Object maskSensitiveValues(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Map) {
            Map<String, Object> result = new HashMap<>();
            Map<?, ?> map = (Map<?, ?>) obj;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                result.put(key, sanitizeValue(key, entry.getValue()));
            }
            return result;
        }
        return obj;
    }
}
