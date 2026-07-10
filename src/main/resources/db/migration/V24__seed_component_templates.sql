-- V24__seed_component_templates.sql
-- Description: 初始化页面构建器(Page Builder)首批14个核心组件物料模板数据。

INSERT INTO cms_component_template (
    component_code, name, category, schema_definition_json, default_props_json, binding_capability_json, status, sort_order, version, created_by, updated_by
) VALUES
('HeroBanner', '首屏主视觉', 'SHOW', 
 '{"fields":[{"fieldKey":"title","label":"主标题","type":"TEXT","required":true,"maxLength":128},{"fieldKey":"subtitle","label":"副标题","type":"TEXTAREA","required":false,"maxLength":256},{"fieldKey":"backgroundMediaId","label":"背景媒体ID","type":"MEDIA","required":false},{"fieldKey":"primaryButtonText","label":"主按钮文本","type":"TEXT","required":false,"maxLength":32},{"fieldKey":"primaryButtonLink","label":"主按钮链接","type":"LINK","required":false,"maxLength":255}]}', 
 '{"title":"让组织拥有持续进化的数字智能能力","subtitle":"数据驱动增长，AI赋能决策","primaryButtonText":"了解解决方案","primaryButtonLink":"/solutions"}', 
 '{"supportedModes":["STATIC"]}', 
 'ACTIVE', 10, 0, 0, 0),

('NavigationBar', '导航栏', 'STRUCTURE', 
 '{"fields":[{"fieldKey":"sticky","label":"是否吸顶","type":"BOOLEAN","defaultValue":true}]}', 
 '{"sticky":true}', 
 '{"supportedModes":["STATIC","AGGREGATE"]}', 
 'ACTIVE', 20, 0, 0, 0),

('MetricCards', '指标卡片组', 'SHOW', 
 '{"fields":[{"fieldKey":"title","label":"模块标题","type":"TEXT","required":false,"maxLength":128}]}', 
 '{"title":"核心业务数据"}', 
 '{"supportedModes":["STATIC","ENTITY"]}', 
 'ACTIVE', 30, 0, 0, 0),

('LogoWall', '客户Logo墙', 'SHOW', 
 '{"fields":[{"fieldKey":"title","label":"模块标题","type":"TEXT","required":false,"maxLength":128},{"fieldKey":"columns","label":"列数","type":"NUMBER","defaultValue":6}]}', 
 '{"title":"服务客户","columns":6}', 
 '{"supportedModes":["STATIC","ENTITY"]}', 
 'ACTIVE', 40, 0, 0, 0),

('CapabilityList', '核心能力列表', 'SHOW', 
 '{"fields":[{"fieldKey":"title","label":"模块标题","type":"TEXT","required":false,"maxLength":128}]}', 
 '{"title":"核心能力底座"}', 
 '{"supportedModes":["STATIC","ENTITY"]}', 
 'ACTIVE', 50, 0, 0, 0),

('ProductGrid', '产品矩阵网格', 'BUSINESS', 
 '{"fields":[{"fieldKey":"title","label":"模块标题","type":"TEXT","required":false,"maxLength":128},{"fieldKey":"limit","label":"限制展示数量","type":"NUMBER","defaultValue":8}]}', 
 '{"title":"产品矩阵","limit":8}', 
 '{"supportedModes":["STATIC","ENTITY"]}', 
 'ACTIVE', 60, 0, 0, 0),

('IndustrySolutionGrid', '行业解决方案网格', 'BUSINESS', 
 '{"fields":[{"fieldKey":"title","label":"模块标题","type":"TEXT","required":false,"maxLength":128}]}', 
 '{"title":"典型行业场景"}', 
 '{"supportedModes":["STATIC","ENTITY"]}', 
 'ACTIVE', 70, 0, 0, 0),

('CaseGrid', '案例展示网格', 'BUSINESS', 
 '{"fields":[{"fieldKey":"title","label":"模块标题","type":"TEXT","required":false,"maxLength":128},{"fieldKey":"limit","label":"限制展示数量","type":"NUMBER","defaultValue":6}]}', 
 '{"title":"标杆案例","limit":6}', 
 '{"supportedModes":["STATIC","ENTITY"]}', 
 'ACTIVE', 80, 0, 0, 0),

('Timeline', '发展历程时间轴', 'SHOW', 
 '{"fields":[{"fieldKey":"title","label":"模块标题","type":"TEXT","required":false,"maxLength":128}]}', 
 '{"title":"发展历程"}', 
 '{"supportedModes":["STATIC","ENTITY"]}', 
 'ACTIVE', 90, 0, 0, 0),

('ValueCardGroup', '核心价值观卡片组', 'SHOW', 
 '{"fields":[{"fieldKey":"title","label":"模块标题","type":"TEXT","required":false,"maxLength":128}]}', 
 '{"title":"核心价值观"}', 
 '{"supportedModes":["STATIC","ENTITY"]}', 
 'ACTIVE', 100, 0, 0, 0),

('TagGroup', '标签组', 'SHOW', 
 '{"fields":[{"fieldKey":"tags","label":"标签列表","type":"TAG_LIST"}]}', 
 '{"tags":["技术过硬","操作简便","功能实用"]}', 
 '{"supportedModes":["STATIC"]}', 
 'ACTIVE', 110, 0, 0, 0),

('ContactInfoBlock', '联系信息展示块', 'SHOW', 
 '{"fields":[{"fieldKey":"title","label":"模块标题","type":"TEXT","required":false,"maxLength":128}]}', 
 '{"title":"关于我们 / 联系我们"}', 
 '{"supportedModes":["STATIC","ENTITY"]}', 
 'ACTIVE', 120, 0, 0, 0),

('ContactLeadForm', '预约留资表单', 'FORM', 
 '{"fields":[{"fieldKey":"title","label":"表单标题","type":"TEXT","defaultValue":"预约交流"},{"fieldKey":"buttonText","label":"按钮文案","type":"TEXT","defaultValue":"提交"}]}', 
 '{"title":"预约交流","buttonText":"提交"}', 
 '{"supportedModes":["STATIC"]}', 
 'ACTIVE', 130, 0, 0, 0),

('RichTextBlock', '富文本区块', 'SHOW', 
 '{"fields":[{"fieldKey":"content","label":"富文本内容","type":"RICH_TEXT","required":true}]}', 
 '{"content":"<p>请在此处编辑富文本内容。</p>"}', 
 '{"supportedModes":["STATIC"]}', 
 'ACTIVE', 140, 0, 0, 0);
