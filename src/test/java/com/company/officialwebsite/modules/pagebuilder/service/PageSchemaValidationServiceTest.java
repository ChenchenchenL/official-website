package com.company.officialwebsite.modules.pagebuilder.service;

import static org.mockito.Mockito.*;

import com.company.officialwebsite.common.enums.ErrorCode;
import com.company.officialwebsite.common.exception.BusinessException;
import com.company.officialwebsite.modules.pagebuilder.model.BindingModel;
import com.company.officialwebsite.modules.pagebuilder.model.ComponentLayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.LayoutModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionResponsiveModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.officialwebsite.modules.pagebuilder.model.PageSchemaModel;
import com.company.officialwebsite.modules.pagebuilder.model.SectionModel;
import com.company.officialwebsite.modules.pagebuilder.model.SeoModel;
import com.company.officialwebsite.modules.pagebuilder.service.impl.PageSchemaValidationServiceImpl;
import com.company.officialwebsite.modules.pagebuilder.vo.ComponentTemplateVO;
import com.company.officialwebsite.modules.media.service.MediaAssetService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

/**
 * PageSchemaValidationServiceTest：验证页面 Schema 配置的字段完整性、组件物料白名单、长度与安全过滤校验。
 */
@ExtendWith(MockitoExtension.class)
class PageSchemaValidationServiceTest {

    @Mock
    private ComponentTemplateService templateService;

    @Mock
    private MediaAssetService mediaAssetService;

    @Mock
    private PageSchemaUpgradeService pageSchemaUpgradeService;

    private ObjectMapper objectMapper = new ObjectMapper();

    private PageSchemaValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new PageSchemaValidationServiceImpl(
                templateService, mediaAssetService, pageSchemaUpgradeService, objectMapper);
    }

    private PageSchemaModel createValidBaseModel() {
        PageSchemaModel model = new PageSchemaModel();
        model.setPageKey("home");
        model.setName("首页");

        LayoutModel layout = new LayoutModel();
        layout.setType("flow");
        model.setLayout(layout);

        SectionModel section = new SectionModel();
        section.setId("section_1");
        section.setComponent("HeroBanner");
        section.setVisible(true);

        Map<String, Object> props = new HashMap<>();
        props.put("title", "测试主标题");
        props.put("backgroundMediaId", "1001");
        props.put("primaryButtonLink", "/solutions");
        section.setProps(props);

        BindingModel binding = new BindingModel();
        binding.setMode("STATIC");
        section.setBinding(binding);

        model.setSections(Collections.singletonList(section));
        return model;
    }

    private ComponentTemplateVO createHeroBannerTemplate() {
        ComponentTemplateVO template = new ComponentTemplateVO();
        template.setComponentCode("HeroBanner");
        template.setName("首屏主视觉");
        template.setStatus("ACTIVE");

        Map<String, Object> schemaDef = new HashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();

        fields.add(createField("title", "TEXT", true, 128));
        fields.add(createField("backgroundMediaId", "MEDIA", false, null));
        fields.add(createField("primaryButtonLink", "LINK", false, 255));

        schemaDef.put("fields", fields);
        template.setSchemaDefinitionJson(schemaDef);

        Map<String, Object> bindingCap = new HashMap<>();
        bindingCap.put("supportedModes", Arrays.asList("STATIC", "ENTITY"));
        template.setBindingCapabilityJson(bindingCap);

        return template;
    }

    private Map<String, Object> createField(String fieldKey, String type, boolean required, Integer maxLength) {
        Map<String, Object> field = new HashMap<>();
        field.put("fieldKey", fieldKey);
        field.put("type", type);
        field.put("required", required);
        if (maxLength != null) {
            field.put("maxLength", maxLength);
        }
        return field;
    }

    @Test
    void validateSchema_shouldSucceed_whenModelIsValid() {
        PageSchemaModel model = createValidBaseModel();
        ComponentTemplateVO template = createHeroBannerTemplate();

        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        Assertions.assertDoesNotThrow(() -> validationService.validateSchema(model));
    }

    @Test
    void validateSchema_shouldThrow_whenPageKeyIsBlank() {
        PageSchemaModel model = createValidBaseModel();
        model.setPageKey("");

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("pageKey"));
    }

    @Test
    void validateSchema_shouldThrow_whenNameTooLong() {
        PageSchemaModel model = createValidBaseModel();
        // Generate a 129 char string
        model.setName(String.join("", Collections.nCopies(130, "a")));

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("长度不能超过128字符"));
    }

    @Test
    void validateSchema_shouldThrow_whenSectionsCountExceedsLimit() {
        PageSchemaModel model = createValidBaseModel();
        List<SectionModel> list = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            SectionModel section = new SectionModel();
            section.setId("sec_" + i);
            section.setComponent("HeroBanner");
            list.add(section);
        }
        model.setSections(list);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("数量不能超过50个"));
    }

    @Test
    void validateSchema_shouldThrow_whenRequiredPropIsMissing() {
        PageSchemaModel model = createValidBaseModel();
        model.getSections().get(0).getProps().remove("title");

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("属性 title 是必选项"));
    }

    @Test
    void validateSchema_shouldThrow_whenLinkProtocolIsInvalid() {
        PageSchemaModel model = createValidBaseModel();
        model.getSections().get(0).getProps().put("primaryButtonLink", "javascript:alert(1)");

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("链接协议不合法"));
    }

    @Test
    void validateSchema_shouldThrow_whenMediaIdIsNegative() {
        PageSchemaModel model = createValidBaseModel();
        model.getSections().get(0).getProps().put("backgroundMediaId", "-10");

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("媒体ID必须为正整数"));
    }

    @Test
    void validateSchema_shouldSanitizeHtml_whenRichTextType() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);
        section.setComponent("RichTextBlock");
        section.getProps().clear();
        section.getProps().put("content", "<p>正常文案<script>alert(1)</script></p>");

        ComponentTemplateVO template = new ComponentTemplateVO();
        template.setComponentCode("RichTextBlock");
        template.setName("富文本区块");
        template.setStatus("ACTIVE");

        Map<String, Object> schemaDef = new HashMap<>();
        List<Map<String, Object>> fields = new ArrayList<>();
        fields.add(createField("content", "RICH_TEXT", true, null));
        schemaDef.put("fields", fields);
        template.setSchemaDefinitionJson(schemaDef);

        when(templateService.getTemplateByCode("RichTextBlock")).thenReturn(template);

        Assertions.assertDoesNotThrow(() -> validationService.validateSchema(model));

        String sanitized = (String) section.getProps().get("content");
        // Script tag must be removed by Jsoup Safelist.basicWithImages()
        Assertions.assertFalse(sanitized.contains("<script>"));
        Assertions.assertTrue(sanitized.contains("<p>正常文案</p>"));
    }

    @Test
    void validateSchema_shouldThrow_whenSeoTitleTooLong() {
        PageSchemaModel model = createValidBaseModel();
        SeoModel seo = new SeoModel();
        seo.setTitle(String.join("", Collections.nCopies(130, "a")));
        model.setSeo(seo);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("SEO标题长度不能超过128字符"));
    }

    @Test
    void validateSchema_shouldThrow_whenSeoDescriptionTooLong() {
        PageSchemaModel model = createValidBaseModel();
        SeoModel seo = new SeoModel();
        seo.setDescription(String.join("", Collections.nCopies(260, "a")));
        model.setSeo(seo);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("SEO描述长度不能超过255字符"));
    }

    @Test
    void validateSchema_shouldThrow_whenBindingModeIsInvalid() {
        PageSchemaModel model = createValidBaseModel();
        model.getSections().get(0).getBinding().setMode("INVALID_MODE");

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("数据绑定模式不合法"));
    }

    @Test
    void validateSchema_shouldThrow_whenBindingModeIsUnsupported() {
        PageSchemaModel model = createValidBaseModel();
        model.getSections().get(0).getBinding().setMode("AGGREGATE");

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("不支持数据绑定模式"));
    }

    @Test
    void validateSchema_shouldThrow_whenPropTypeIsNotString() {
        PageSchemaModel model = createValidBaseModel();
        // title must be TEXT (String), but we pass an Integer 123
        model.getSections().get(0).getProps().put("title", 123);

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("必须为字符串类型"));
    }

    @Test
    void validateSchema_shouldThrow_whenBindingSourceIsNotWhitelisted() {
        PageSchemaModel model = createValidBaseModel();
        model.getSections().get(0).getBinding().setMode("ENTITY");
        model.getSections().get(0).getBinding().setSource("invalid_binding_source");

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("数据绑定源不合法"));
    }

    @Test
    void validateSchema_shouldThrow_whenMediaAssetIsInvalid() {
        PageSchemaModel model = createValidBaseModel();
        model.getSections().get(0).getProps().put("backgroundMediaId", "999");

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);
        doThrow(new RuntimeException("invalid")).when(mediaAssetService).requireUsableImage(999L);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("绑定的媒体资源不可用或不存在"));
    }

    @Test
    void validateSchema_shouldSucceed_whenLayoutAndStyleAreValid() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        ComponentLayoutModel layout = new ComponentLayoutModel();
        layout.setPosition("absolute");
        layout.setX(120);
        layout.setY(48);
        layout.setWidth(360);
        layout.setHeight(72);
        layout.setZIndex(2);
        section.setLayout(layout);

        Map<String, Object> style = new HashMap<>();
        style.put("color", "#111111");
        style.put("fontSize", "24px");
        style.put("textAlign", "left");
        style.put("opacity", 0.9);
        section.setStyle(style);

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        Assertions.assertDoesNotThrow(() -> validationService.validateSchema(model));
    }

    @Test
    void validateSchema_shouldThrow_whenPageLayoutTypeIsUnsupported() {
        PageSchemaModel model = createValidBaseModel();
        model.getLayout().setType("custom_unsupported_mode");

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("不支持的页面布局模式"));
    }

    @Test
    void validateSchema_shouldThrow_whenComponentZIndexIsExceeded() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        ComponentLayoutModel layout = new ComponentLayoutModel();
        layout.setZIndex(99999);
        section.setLayout(layout);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("zIndex 超出允许范围"));
    }

    @Test
    void validateSchema_shouldThrow_whenStyleContainsDangerousScript() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        Map<String, Object> style = new HashMap<>();
        style.put("color", "red; background: url('http://evil.com')");
        section.setStyle(style);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("包含非法危险脚本字符") || ex.getMessage().contains("颜色格式不合法"));
    }

    @Test
    void validateSchema_shouldThrow_whenStyleAttributeIsNotWhitelisted() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        Map<String, Object> style = new HashMap<>();
        style.put("evilNonWhitelistedProp", "some_val");
        section.setStyle(style);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("未允许的样式属性"));
    }

    @Test
    void validateSchema_shouldThrow_whenSchemaByteSizeExceeds512KB() {
        PageSchemaModel model = createValidBaseModel();
        // Generate a huge text attribute > 512KB
        model.setName("a".repeat(530000));

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("体积超出限制"));
    }

    @Test
    void validateSchema_shouldThrow_whenCoordinateExceedsLimit() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        ComponentLayoutModel layout = new ComponentLayoutModel();
        layout.setX(20000);
        section.setLayout(layout);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("超出允许范围"));
    }

    @Test
    void validateSchema_shouldThrow_whenOpacityIsInvalid() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        Map<String, Object> style = new HashMap<>();
        style.put("opacity", 2.5);
        section.setStyle(style);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("透明度(opacity)必须在 [0.0, 1.0] 范围内"));
    }

    @Test
    void validateSchema_shouldThrow_whenPositionIsInvalid() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        ComponentLayoutModel layout = new ComponentLayoutModel();
        layout.setPosition("invalid_position_type");
        section.setLayout(layout);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("定位模式(position)不合法"));
    }

    @Test
    void validateSchema_shouldSucceed_whenResponsiveBreakpointsAreValid() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        Map<String, SectionResponsiveModel> responsive = new HashMap<>();

        SectionResponsiveModel mobileResp = new SectionResponsiveModel();
        ComponentLayoutModel mobileLayout = new ComponentLayoutModel();
        mobileLayout.setPosition("relative");
        mobileLayout.setWidth("100%");
        mobileResp.setLayout(mobileLayout);

        Map<String, Object> mobileStyle = new HashMap<>();
        mobileStyle.put("fontSize", "16px");
        mobileResp.setStyle(mobileStyle);

        responsive.put("mobile", mobileResp);
        section.setResponsive(responsive);

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        Assertions.assertDoesNotThrow(() -> validationService.validateSchema(model));
    }

    @Test
    void validateSchema_shouldThrow_whenBreakpointKeyIsInvalid() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        Map<String, SectionResponsiveModel> responsive = new HashMap<>();
        responsive.put("invalid_breakpoint_key", new SectionResponsiveModel());
        section.setResponsive(responsive);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("响应式断点标识不合法"));
    }

    @Test
    void validateSchema_shouldThrow_whenBreakpointKeyIsEmpty() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        Map<String, SectionResponsiveModel> responsive = new HashMap<>();
        responsive.put("   ", new SectionResponsiveModel());
        section.setResponsive(responsive);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("响应式断点标识不能为空"));
    }

    @Test
    void validateSchema_shouldThrow_whenResponsiveLayoutZIndexIsExceeded() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        Map<String, SectionResponsiveModel> responsive = new HashMap<>();
        SectionResponsiveModel mobileResp = new SectionResponsiveModel();
        ComponentLayoutModel mobileLayout = new ComponentLayoutModel();
        mobileLayout.setZIndex(99999);
        mobileResp.setLayout(mobileLayout);

        responsive.put("mobile", mobileResp);
        section.setResponsive(responsive);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("zIndex 超出允许范围"));
    }

    @Test
    void validateSchema_shouldThrow_whenResponsiveStyleContainsDangerousScript() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        Map<String, SectionResponsiveModel> responsive = new HashMap<>();
        SectionResponsiveModel mobileResp = new SectionResponsiveModel();
        Map<String, Object> mobileStyle = new HashMap<>();
        mobileStyle.put("color", "red; background: url('http://evil.com')");
        mobileResp.setStyle(mobileStyle);

        responsive.put("mobile", mobileResp);
        section.setResponsive(responsive);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("包含非法危险脚本字符") || ex.getMessage().contains("颜色格式不合法"));
    }

    @Test
    void validateSchema_shouldSucceed_whenBindingQueryIsValid() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("product");

        Map<String, Object> query = new HashMap<>();
        query.put("id", 100L);
        query.put("categoryId", 10L);
        query.put("limit", 10);
        query.put("sortOrder", "LATEST");
        binding.setQuery(query);
        section.setBinding(binding);

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        Assertions.assertDoesNotThrow(() -> validationService.validateSchema(model));
    }

    @Test
    void validateSchema_shouldThrow_whenBindingQueryKeyIsNotWhitelisted() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("product");

        Map<String, Object> query = new HashMap<>();
        query.put("customSQLKey", "SELECT * FROM users");
        binding.setQuery(query);
        section.setBinding(binding);

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("包含非允许的参数Key"));
    }

    @Test
    void validateSchema_shouldThrow_whenBindingQueryIdsExceedLimit() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("product");

        Map<String, Object> query = new HashMap<>();
        List<Long> hugeIds = new ArrayList<>();
        for (long i = 1; i <= 51; i++) {
            hugeIds.add(i);
        }
        query.put("ids", hugeIds);
        binding.setQuery(query);
        section.setBinding(binding);

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("单次最多查询 50 个 ID"));
    }

    @Test
    void validateSchema_shouldThrow_whenBindingQueryLimitExceeds50() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("product");

        Map<String, Object> query = new HashMap<>();
        query.put("limit", 100);
        binding.setQuery(query);
        section.setBinding(binding);

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("必须在 [1, 50] 范围内"));
    }

    @Test
    void validateSchema_shouldThrow_whenBindingQuerySortOrderIsInvalid() {
        PageSchemaModel model = createValidBaseModel();
        SectionModel section = model.getSections().get(0);

        BindingModel binding = new BindingModel();
        binding.setMode("ENTITY");
        binding.setSource("product");

        Map<String, Object> query = new HashMap<>();
        query.put("sortOrder", "INVALID_SORT_ENUM");
        binding.setQuery(query);
        section.setBinding(binding);

        ComponentTemplateVO template = createHeroBannerTemplate();
        when(templateService.getTemplateByCode("HeroBanner")).thenReturn(template);

        BusinessException ex = Assertions.assertThrows(
                BusinessException.class,
                () -> validationService.validateSchema(model)
        );
        Assertions.assertEquals(ErrorCode.COMMON_PARAM_INVALID, ex.getErrorCode());
        Assertions.assertTrue(ex.getMessage().contains("排序方式不合法"));
    }
}
