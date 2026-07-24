# P2-2 版本差异与发布审阅实施方案 (plan.md)

本文档详细定义低代码官网后端 **P2-2 版本差异与发布审阅** 的方案选型、数据模型、Schema 递归 Diff 算法、脱敏与安全规约、Admin API 契约、技术拆解、预计难点与解决办法、边界条件及代码改造规范。

---

## 一、方案选型与架构设计 (Architecture & Design Selection)

### 1. 后端 Diff 计算与审阅模型选型
虽然前端可以实现基础 JSON 比较，但发布与回滚决策涉及后台内容安全与审计门禁。为确保敏感参数（如内部查询逻辑、文件路径、Token）不外泄，同时让管理员在发布/回滚前清晰掌控页面配置变更，系统采用 **后端安全脱敏计算 Diff & 结构化发布审阅摘要** 方案：

* **核心能力 1 (Schema Safe Diff)**：后端对两份 `PageSchemaModel` 递归比对，提取组件增删改明细，归一化为 `path`, `changeType`, `fieldName`, `oldValue`, `newValue`。
* **核心能力 2 (Publish Review Summary)**：在管理员点击“发布”前，提供发布预审概览，聚合对比版本号、Schema Hash、组件总数变化、绑定数据源摘要、预校验结果与修改列表。

### 2. 脱敏与安全规则 (Security & Masking Rules)
1. **脱敏过滤**：Diff 计算过程中，若涉及到敏感绑定参数或系统隐式属性，仅保留白名单受控参数 (`categoryId`, `ids`, `limit`, `pageSize`, `sortOrder`, `orderBy`)；彻底擦除内部路径、授权凭证或调试元信息。
2. **鉴权门禁**：版本差异与发布审阅接口仅限 `ADMINISTRATOR` 角色访问，Portal 前台与普通只读视角不可访问。

---

## 二、核心对象与数据模型 (Core Domain Objects)

### 1. 结构化变更项模型 (`SchemaDiffItemVO`)
```java
public class SchemaDiffItemVO {
    private String path;           // 变动路径, 例如 "sections[hero].style.color"
    private String componentId;    // 组件 Section ID
    private String componentCode;  // 组件编码, 例如 "Heading"
    private String changeType;     // 变更类型: "ADDED", "REMOVED", "MODIFIED"
    private String fieldName;      // 变更字段名称, 例如 "color"
    private Object oldValue;       // 旧值 (脱敏后)
    private Object newValue;       // 新值 (脱敏后)
}
```

### 2. 发布审阅综合概览模型 (`PublishReviewVO`)
```java
public class PublishReviewVO {
    private Long pageId;
    private String pageName;
    private Integer draftVersion;         // 草稿当前乐观锁版本
    private String draftSchemaHash;      // 当前草稿 Schema Hash
    private Integer activeVersion;        // 当前在线发布版本 (若未发布过为 null)
    private String activeSchemaHash;     // 当前在线快照 Hash
    private int draftSectionCount;       // 草稿组件区块数量
    private int activeSectionCount;      // 已发布在线组件区块数量
    private List<String> bindingSources; // 草稿使用的受控数据绑定源摘要列表
    private boolean validationPassed;    // 发布前 Schema 预校验是否通过
    private String validationErrorMessage;// 若未通过的具体提示信息
    private List<SchemaDiffItemVO> diffItems; // 变更细项列表
}
```

---

## 三、Admin 接口契约设计 (Admin API Contracts)

1. **获取页面版本差异 (Diff)**：
   * `GET /admin/api/page-builder/pages/{pageId}/diff`
   * Query 参数：`compareVersion` (选填, 指定版本号；不传时默认与在线 ACTIVE 快照比对)。
   * 返回：`List<SchemaDiffItemVO>` 变更明细列表。
2. **获取发布前审阅概览 (Publish Review)**：
   * `GET /admin/api/page-builder/pages/{pageId}/publish-review`
   * 返回：`PublishReviewVO` 完整发布预审视图。

---

## 四、技术拆解 (Technical Breakdown)

```mermaid
graph TD
    A[管理员访问发布审阅页] --> B[GET /pages/{pageId}/publish-review]
    B --> C[PageDiffService.generatePublishReview]
    C --> D[1. 读取草稿 Schema & 当前 ACTIVE 快照 Schema]
    D --> E[2. 执行 PageSchemaValidationService.validateSchema 预校验]
    E --> F[3. PageSchemaDiffHelper.compareSchemas 提取 Diff 列表]
    F --> G[4. 过滤敏感 key & 提取绑定源摘要]
    G --> H[返回完整 PublishReviewVO 供管理员审阅决策]
```

---

## 五、预计难点与解决办法

### 难点 1：数组/组件区块顺序调整导致全量误报为 MODIFIED / REMOVED
* **场景与风险**：前端调整了组件区块的顺序（Section `id` 相同，但数组索引改变），若直接按数组 index 比较，会导致所有位置错位，将后续所有组件误判为被修改。
* **解决办法**：在 `PageSchemaDiffHelper` 中，将组件 `sections` 列表按 `id` 建立 Map；通过组件 `id` 进行精确节点匹配，识别 `ADDED`、`REMOVED` 与 `MODIFIED`；若仅位置变动，单独标记 `layout` 变更或顺序调整。

### 难点 2：敏感信息泄露防范
* **场景与风险**：某些组件可能会挂载内部系统凭证或服务器存储绝对路径。
* **解决办法**：在 Diff 计算与格式化输出时，建立属性 Key 敏感词过滤门禁（黑名单如 `token`, `password`, `secret`, `filepath`, `absPath`），对敏感 key 掩码化为 `"******"` 或过滤排除。

---

## 六、边界条件分析 (Boundary Conditions)

1. **页面尚未发布过（没有在线 ACTIVE 快照）时的比对与发布审阅**：
   * `activeVersion` 与 `activeSchemaHash` 返回 `null`；`diffItems` 中所有当前草稿组件节点均识别为 `ADDED`；审阅通过，提示“全新页面发布”。
2. **草稿 Schema 包含预校验错误（如 zIndex 超界）时获取发布审阅**：
   * `validationPassed` 返回 `false`，并在 `validationErrorMessage` 中输出具体的预校验报错（例如：“组件 Heading 的 zIndex 超出允许范围”），阻断误操作发布。
