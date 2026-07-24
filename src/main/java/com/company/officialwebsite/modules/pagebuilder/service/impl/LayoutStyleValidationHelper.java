package com.company.officialwebsite.modules.pagebuilder.service.impl;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.model.ComponentLayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionResponsiveModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * LayoutStyleValidationHelper: 页面与组件布局、样式及容量配额白名单校验工具类。
 */
public class LayoutStyleValidationHelper {

    private static final Logger log = LoggerFactory.getLogger(LayoutStyleValidationHelper.class);

    public static final int MAX_SCHEMA_BYTES = 524288; // 512 KB
    public static final int MAX_SECTIONS_COUNT = 50;
    public static final int MAX_SINGLE_TEXT_LENGTH = 10000;

    public static final Set<String> ALLOWED_BREAKPOINTS = Set.of("desktop", "tablet", "mobile");
    private static final Set<String> ALLOWED_PAGE_LAYOUT_TYPES = Set.of("flow", "grid", "absolute", "default");
    private static final Set<String> ALLOWED_POSITION_TYPES = Set.of("static", "relative", "absolute", "fixed", "sticky");

    // 安全 CSS 长度/尺寸单位正则
    private static final Pattern CSS_LENGTH_PATTERN = Pattern.compile("^([+-]?\\d+(\\.\\d+)?(px|em|rem|%|vw|vh)|auto|fit-content|max-content|min-content)$", Pattern.CASE_INSENSITIVE);

    // 颜色格式正则 (Hex #RGB, #RRGGBB, #RRGGBBAA; rgb/rgba; hsl/hsla; 常用安全关键字)
    private static final Pattern COLOR_PATTERN = Pattern.compile("^#([0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$|^rgba?\\s*\\([^\\);\\{\\}]+\\)$|^hsla?\\s*\\([^\\);\\{\\}]+\\)$|^(transparent|inherit|initial|unset)$", Pattern.CASE_INSENSITIVE);

    // 标识符安全正则 (仅允许字母、数字、下划线、短横线及空格)
    private static final Pattern SAFE_IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\s]+$");

    // 组件样式属性白名单
    private static final Set<String> ALLOWED_STYLE_KEYS = Set.of(
            "fontSize", "fontWeight", "fontFamily", "fontStyle", "textAlign", "lineHeight", "letterSpacing",
            "color", "backgroundColor", "borderColor", "borderWidth", "borderStyle", "borderRadius",
            "padding", "paddingTop", "paddingRight", "paddingBottom", "paddingLeft",
            "margin", "marginTop", "marginRight", "marginBottom", "marginLeft",
            "width", "height", "minWidth", "maxWidth", "minHeight", "maxHeight",
            "boxShadow", "opacity", "display", "flexDirection", "justifyContent", "alignItems", "gap",
            "flexWrap", "flexShrink", "flexGrow", "className", "animationName", "overflow", "cursor"
    );

    /**
     * 校验 Schema 物理容量与配额上限 (JSON 字节数 <= 512KB, 区块数 <= 50, 单属性文本 <= 10000 字符)。
     */
    public static void validateQuota(PageSchemaModel model, ObjectMapper objectMapper) {
        if (model == null) {
            return;
        }

        try {
            byte[] bytes = objectMapper.writeValueAsString(model).getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_SCHEMA_BYTES) {
                logAndThrow("页面 Schema 配置体积超出限制 (当前: " + bytes.length + " Bytes, 上限: " + MAX_SCHEMA_BYTES + " Bytes)");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to serialize schema for byte size check", e);
        }

        List<SectionModel> sections = model.getSections();
        if (sections != null && sections.size() > MAX_SECTIONS_COUNT) {
            logAndThrow("单个页面的组件区块数量不能超过 " + MAX_SECTIONS_COUNT + " 个");
        }
    }

    /**
     * 校验页面级 LayoutModel 布局配置。
     */
    public static void validatePageLayout(LayoutModel layout) {
        if (layout == null || layout.getType() == null || layout.getType().trim().isEmpty()) {
            logAndThrow("页面布局配置(layout.type)不正确");
        }
        String type = layout.getType().trim().toLowerCase();
        if (!ALLOWED_PAGE_LAYOUT_TYPES.contains(type)) {
            logAndThrow("不支持的页面布局模式: " + layout.getType() + "，合法模式为: " + ALLOWED_PAGE_LAYOUT_TYPES);
        }
    }

    /**
     * 校验组件级 ComponentLayoutModel 布局配置。
     */
    public static void validateComponentLayout(ComponentLayoutModel layout, String componentCode) {
        if (layout == null) {
            return;
        }

        // 1. 定位模式校验
        if (layout.getPosition() != null && !layout.getPosition().trim().isEmpty()) {
            String pos = layout.getPosition().trim().toLowerCase();
            if (!ALLOWED_POSITION_TYPES.contains(pos)) {
                logAndThrow("组件 " + componentCode + " 的定位模式(position)不合法: " + layout.getPosition());
            }
        }

        // 2. 坐标 x, y 校验
        validateCoordinate(layout.getX(), "x", componentCode, -10000, 10000);
        validateCoordinate(layout.getY(), "y", componentCode, -10000, 10000);

        // 3. 尺寸 width, height 校验
        validateDimension(layout.getWidth(), "width", componentCode, 0, 10000);
        validateDimension(layout.getHeight(), "height", componentCode, 0, 10000);

        // 4. 层级 zIndex 校验
        if (layout.getZIndex() != null) {
            int zIndex = layout.getZIndex();
            if (zIndex < -100 || zIndex > 9999) {
                logAndThrow("组件 " + componentCode + " 的 zIndex 超出允许范围 [-100, 9999]: " + zIndex);
            }
        }
    }

    /**
     * 校验组件级 style 属性白名单、色彩正则与标识符安全。
     */
    public static void validateComponentStyle(Map<String, Object> style, String componentCode) {
        if (style == null || style.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : style.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }

            // 白名单属性名检查
            if (!ALLOWED_STYLE_KEYS.contains(key)) {
                logAndThrow("组件 " + componentCode + " 包含未允许的样式属性: " + key);
            }

            String valStr = value.toString().trim();

            // 针对恶意注入关键字的防线：严禁包含 ';' 或 '{' 或 '}' 或 'javascript:' 或 'expression('
            String valLower = valStr.toLowerCase();
            if (valLower.contains(";") || valLower.contains("{") || valLower.contains("}")
                    || valLower.contains("javascript:") || valLower.contains("expression(")) {
                logAndThrow("组件 " + componentCode + " 的样式属性 " + key + " 包含非法危险脚本字符");
            }

            // 色彩属性校验
            if (key.equals("color") || key.equals("backgroundColor") || key.equals("borderColor")) {
                if (!COLOR_PATTERN.matcher(valStr).matches()) {
                    logAndThrow("组件 " + componentCode + " 的样式属性 " + key + " 颜色格式不合法: " + valStr);
                }
            }

            // 透明度校验
            if (key.equals("opacity")) {
                try {
                    double opacity = Double.parseDouble(valStr);
                    if (opacity < 0.0 || opacity > 1.0) {
                        logAndThrow("组件 " + componentCode + " 的透明度(opacity)必须在 [0.0, 1.0] 范围内: " + valStr);
                    }
                } catch (NumberFormatException e) {
                    logAndThrow("组件 " + componentCode + " 的透明度(opacity)必须为数字: " + valStr);
                }
            }

            // 标识符安全校验 (className, fontFamily, animationName)
            if (key.equals("className") || key.equals("fontFamily") || key.equals("animationName")) {
                if (!SAFE_IDENTIFIER_PATTERN.matcher(valStr).matches()) {
                    logAndThrow("组件 " + componentCode + " 的样式属性 " + key + " 包含非法特殊字符: " + valStr);
                }
            }
        }
    }

    /**
     * 校验组件响应式断点 (responsive) 配置白名单及各断点的 layout/style 重写参数。
     */
    public static void validateSectionResponsive(Map<String, SectionResponsiveModel> responsive, String componentCode) {
        if (responsive == null || responsive.isEmpty()) {
            return;
        }

        for (Map.Entry<String, SectionResponsiveModel> entry : responsive.entrySet()) {
            String breakpointKey = entry.getKey();
            if (breakpointKey == null || breakpointKey.trim().isEmpty()) {
                logAndThrow("组件 " + componentCode + " 的响应式断点标识不能为空");
            }
            String bpLower = breakpointKey.trim().toLowerCase();
            if (!ALLOWED_BREAKPOINTS.contains(bpLower)) {
                logAndThrow("组件 " + componentCode + " 的响应式断点标识不合法: " + breakpointKey + "，合法断点为: " + ALLOWED_BREAKPOINTS);
            }

            SectionResponsiveModel model = entry.getValue();
            if (model == null) {
                continue;
            }

            // 针对该断点的 ComponentLayout 进行递归校验
            if (model.getLayout() != null) {
                validateComponentLayout(model.getLayout(), componentCode + "[" + bpLower + "]");
            }

            // 针对该断点的 ComponentStyle 进行递归校验
            if (model.getStyle() != null) {
                validateComponentStyle(model.getStyle(), componentCode + "[" + bpLower + "]");
            }
        }
    }

    private static void validateCoordinate(Object val, String propName, String componentCode, int min, int max) {
        if (val == null) {
            return;
        }
        if (val instanceof Number) {
            double num = ((Number) val).doubleValue();
            if (num < min || num > max) {
                logAndThrow("组件 " + componentCode + " 的布局属性 " + propName + " 超出允许范围 [" + min + ", " + max + "]: " + num);
            }
        } else if (val instanceof String) {
            String str = ((String) val).trim();
            if (!CSS_LENGTH_PATTERN.matcher(str).matches()) {
                logAndThrow("组件 " + componentCode + " 的布局属性 " + propName + " CSS 长度格式不合法: " + str);
            }
        }
    }

    private static void validateDimension(Object val, String propName, String componentCode, int min, int max) {
        if (val == null) {
            return;
        }
        if (val instanceof Number) {
            double num = ((Number) val).doubleValue();
            if (num < min || num > max) {
                logAndThrow("组件 " + componentCode + " 的尺寸属性 " + propName + " 超出允许范围 [" + min + ", " + max + "]: " + num);
            }
        } else if (val instanceof String) {
            String str = ((String) val).trim();
            if (!CSS_LENGTH_PATTERN.matcher(str).matches()) {
                logAndThrow("组件 " + componentCode + " 的尺寸属性 " + propName + " CSS 长度格式不合法: " + str);
            }
        }
    }

    private static void logAndThrow(String message) {
        log.warn("layout & style validation failed: {}", message);
        throw new BusinessException(ErrorCode.COMMON_PARAM_INVALID, message);
    }
}
