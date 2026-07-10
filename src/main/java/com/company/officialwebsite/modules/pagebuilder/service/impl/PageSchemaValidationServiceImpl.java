package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.enums.BindingTypeEnum;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.model.SeoModel;
import com.company.officialwebsite.modules.pagebuilder.service.ComponentTemplateService;
import com.company.officialwebsite.modules.pagebuilder.service.PageSchemaValidationService;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * PageSchemaValidationServiceImpl: 页面 Schema 配置合法性校验服务实现类。
 */
@Service
public class PageSchemaValidationServiceImpl implements PageSchemaValidationService {

    private static final Logger log = LoggerFactory.getLogger(PageSchemaValidationServiceImpl.class);

    // Constant validation messages to prevent hardcoding (resolves H-10)
    private static final String MSG_SCHEMA_NULL = "页面配置不能为空";
    private static final String MSG_PAGE_KEY_BLANK = "页面唯一Key标识(pageKey)不能为空";
    private static final String MSG_PAGE_KEY_TOO_LONG = "页面唯一Key标识(pageKey)长度不能超过64字符";
    private static final String MSG_PAGE_NAME_BLANK = "页面名称(name)不能为空";
    private static final String MSG_PAGE_NAME_TOO_LONG = "页面名称(name)长度不能超过128字符";
    private static final String MSG_LAYOUT_INVALID = "页面布局配置(layout)不正确";
    private static final String MSG_SECTIONS_EMPTY = "页面必须至少配置一个组件区块(sections)";
    private static final String MSG_SECTIONS_TOO_MANY = "单个页面的组件区块数量不能超过50个";
    private static final String MSG_SEO_TITLE_TOO_LONG = "SEO标题长度不能超过128字符";
    private static final String MSG_SEO_DESC_TOO_LONG = "SEO描述长度不能超过255字符";
    private static final String MSG_SECTION_ID_BLANK = "区块组件节点实例唯一ID(id)不能为空";
    private static final String MSG_SECTION_COMP_BLANK = "区块组件类型编码(component)不能为空";
    private static final String MSG_SECTION_PROPS_NULL_FMT = "组件 %s 的属性配置(props)不能为空";
    private static final String MSG_PROP_REQUIRED_FMT = "组件 %s 的属性 %s 是必选项";
    private static final String MSG_PROP_NOT_STRING_FMT = "组件 %s 的属性 %s 必须为字符串类型";
    private static final String MSG_PROP_TOO_LONG_FMT = "组件 %s 的属性 %s 长度不能超过 %d";
    private static final String MSG_PROP_LINK_INVALID_FMT = "组件 %s 的属性 %s 链接协议不合法，必须以 http://, https://, mailto: 或 / 开头";
    private static final String MSG_PROP_MEDIA_INVALID_FMT = "组件 %s 的属性 %s 媒体ID格式不合法";
    private static final String MSG_PROP_MEDIA_NEGATIVE_FMT = "组件 %s 的属性 %s 媒体ID必须为正整数";
    private static final String MSG_BIND_MODE_INVALID_FMT = "组件 %s 的数据绑定模式不合法: %s";
    private static final String MSG_BIND_MODE_UNSUPPORTED_FMT = "组件 %s 不支持数据绑定模式: %s";

    private static final Set<String> ALLOWED_BINDING_SOURCES = Set.of(
            "site_config", "navigation_menu", "home_metric_card", "product",
            "industry_solution", "case", "timeline_event", "value_card",
            "promise_tag", "contact_info", "cooperation_direction_tag"
    );

    private final ComponentTemplateService templateService;
    private final MediaAssetService mediaAssetService;

    public PageSchemaValidationServiceImpl(
            ComponentTemplateService templateService,
            MediaAssetService mediaAssetService) {
        this.templateService = templateService;
        this.mediaAssetService = mediaAssetService;
    }

    @Override
    public void validateSchema(PageSchemaModel model) {
        if (model == null) {
            logAndThrow(MSG_SCHEMA_NULL);
        }

        // 1. 基础字段非空及长度校验
        if (model.getPageKey() == null || model.getPageKey().trim().isEmpty()) {
            logAndThrow(MSG_PAGE_KEY_BLANK);
        }
        if (model.getPageKey().length() > 64) {
            logAndThrow(MSG_PAGE_KEY_TOO_LONG);
        }

        if (model.getName() == null || model.getName().trim().isEmpty()) {
            logAndThrow(MSG_PAGE_NAME_BLANK);
        }
        if (model.getName().length() > 128) {
            logAndThrow(MSG_PAGE_NAME_TOO_LONG);
        }

        if (model.getLayout() == null || model.getLayout().getType() == null || model.getLayout().getType().trim().isEmpty()) {
            logAndThrow(MSG_LAYOUT_INVALID);
        }

        // 2. SEO 信息校验 (resolves RW-4)
        SeoModel seo = model.getSeo();
        if (seo != null) {
            if (seo.getTitle() != null && seo.getTitle().length() > 128) {
                logAndThrow(MSG_SEO_TITLE_TOO_LONG);
            }
            if (seo.getDescription() != null && seo.getDescription().length() > 255) {
                logAndThrow(MSG_SEO_DESC_TOO_LONG);
            }
        }

        // 3. 嵌套/组件数量超限校验 (限制最大50个区块)
        List<SectionModel> sections = model.getSections();
        if (sections == null || sections.isEmpty()) {
            logAndThrow(MSG_SECTIONS_EMPTY);
        }
        if (sections.size() > 50) {
            logAndThrow(MSG_SECTIONS_TOO_MANY);
        }

        // 4. 各组件及内部属性的白名单、类型、长度与安全校验
        for (SectionModel section : sections) {
            validateSection(section);
        }
    }

    private void validateSection(SectionModel section) {
        if (section.getId() == null || section.getId().trim().isEmpty()) {
            logAndThrow(MSG_SECTION_ID_BLANK);
        }

        String componentCode = section.getComponent();
        if (componentCode == null || componentCode.trim().isEmpty()) {
            logAndThrow(MSG_SECTION_COMP_BLANK);
        }

        // 4.1 校验组件是否注册且启用
        ComponentTemplateVO template = templateService.getTemplateByCode(componentCode);

        // 4.2 校验数据绑定模型 (resolves I-13)
        BindingModel binding = section.getBinding();
        if (binding != null && binding.getMode() != null) {
            String modeStr = binding.getMode().trim();
            if (!modeStr.isEmpty()) {
                // 校验绑定模式是否为合法枚举
                BindingTypeEnum modeEnum;
                try {
                    modeEnum = BindingTypeEnum.valueOf(modeStr.toUpperCase());
                } catch (IllegalArgumentException e) {
                    logAndThrow(String.format(MSG_BIND_MODE_INVALID_FMT, componentCode, modeStr));
                }

                // 校验绑定数据源是否在白名单中
                String source = binding.getSource();
                if (source != null && !source.trim().isEmpty()) {
                    String sourceLower = source.trim().toLowerCase();
                    if (!ALLOWED_BINDING_SOURCES.contains(sourceLower)) {
                        logAndThrow(String.format("组件 %s 的数据绑定源不合法: %s", componentCode, source));
                    }
                }

                // 校验组件模板是否支持该绑定模式
                Map<String, Object> bindingCap = template.getBindingCapabilityJson();
                if (bindingCap != null && bindingCap.containsKey("supportedModes")) {
                    Object modesObj = bindingCap.get("supportedModes");
                    if (modesObj instanceof List) {
                        List<?> supportedModes = (List<?>) modesObj;
                        if (!supportedModes.contains(modeStr) && !supportedModes.contains(modeStr.toUpperCase())) {
                            logAndThrow(String.format(MSG_BIND_MODE_UNSUPPORTED_FMT, componentCode, modeStr));
                        }
                    }
                }
            }
        }

        // 4.3 校验组件属性及样式
        Map<String, Object> props = section.getProps();
        if (props == null) {
            logAndThrow(String.format(MSG_SECTION_PROPS_NULL_FMT, componentCode));
        }

        Map<String, Object> schemaDef = template.getSchemaDefinitionJson();
        if (schemaDef != null && schemaDef.containsKey("fields")) {
            Object fieldsObj = schemaDef.get("fields");
            if (fieldsObj instanceof List) {
                List<?> fieldsList = (List<?>) fieldsObj;
                for (Object fieldObj : fieldsList) {
                    if (fieldObj instanceof Map) {
                        Map<?, ?> fieldMap = (Map<?, ?>) fieldObj;
                        String fieldKey = (String) fieldMap.get("fieldKey");
                        String type = (String) fieldMap.get("type");
                        Boolean required = getAsBoolean(fieldMap.get("required"));
                        Integer maxLength = getAsInteger(fieldMap.get("maxLength"));

                        Object val = props.get(fieldKey);

                        // 必填项校验
                        if (Boolean.TRUE.equals(required)) {
                            if (val == null || val.toString().trim().isEmpty()) {
                                logAndThrow(String.format(MSG_PROP_REQUIRED_FMT, componentCode, fieldKey));
                            }
                        }

                        if (val != null) {
                            // TEXT/TEXTAREA/LINK/RICH_TEXT 类型必须是 String (resolves I-12, RW-5)
                            if ("TEXT".equals(type) || "TEXTAREA".equals(type) || "LINK".equals(type) || "RICH_TEXT".equals(type)) {
                                if (!(val instanceof String)) {
                                    logAndThrow(String.format(MSG_PROP_NOT_STRING_FMT, componentCode, fieldKey));
                                }
                            }

                            String valStr = val.toString();

                            // 长度校验
                            if (maxLength != null && valStr.length() > maxLength) {
                                logAndThrow(String.format(MSG_PROP_TOO_LONG_FMT, componentCode, fieldKey, maxLength));
                            }

                            // 链接协议校验
                            if ("LINK".equals(type)) {
                                String link = valStr.trim();
                                if (!link.isEmpty()) {
                                    if (!link.startsWith("http://") && !link.startsWith("https://") && !link.startsWith("mailto:") && !link.startsWith("/")) {
                                        logAndThrow(String.format(MSG_PROP_LINK_INVALID_FMT, componentCode, fieldKey));
                                    }
                                }
                            }

                            // 媒体ID格式及存在性校验
                            if ("MEDIA".equals(type)) {
                                String mediaStr = valStr.trim();
                                if (!mediaStr.isEmpty()) {
                                    try {
                                        long mediaId = Long.parseLong(mediaStr);
                                        if (mediaId <= 0) {
                                            logAndThrow(String.format(MSG_PROP_MEDIA_NEGATIVE_FMT, componentCode, fieldKey));
                                        }
                                        try {
                                            mediaAssetService.requireUsableImage(mediaId);
                                        } catch (Exception e) {
                                            logAndThrow(String.format("组件 %s 的属性 %s 绑定的媒体资源不可用或不存在: %d", componentCode, fieldKey, mediaId));
                                        }
                                    } catch (NumberFormatException e) {
                                        logAndThrow(String.format(MSG_PROP_MEDIA_INVALID_FMT, componentCode, fieldKey));
                                    }
                                }
                            }

                            // 富文本安全校验与清洗 (XSS)
                            if ("RICH_TEXT".equals(type)) {
                                String richText = valStr;
                                String cleaned = Jsoup.clean(richText, Safelist.basicWithImages());
                                props.put(fieldKey, cleaned);
                            }
                        }
                    }
                }
            }
        }
    }

    private void logAndThrow(String message) {
        log.warn("page schema validation failed: {}", message);
        throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, message);
    }

    private Integer getAsInteger(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        try {
            return Integer.parseInt(obj.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Boolean getAsBoolean(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj instanceof Boolean) {
            return (Boolean) obj;
        }
        return Boolean.parseBoolean(obj.toString());
    }
}
