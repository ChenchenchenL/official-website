# P1-1 Schema 版本与兼容策略任务拆解与自检清单 (task.md)

本文档将 **P1-1 Schema 版本与兼容策略整改** 拆化为具体的开发与测试任务清单，并提供自检门禁。

---

## 一、任务细分拆解清单 (Step-by-Step Task Breakdown)

### 阶段一：模型扩展与升级服务实现

- [ ] **Task 1.1**: 更新 `PageSchemaModel` 类：
  * 在顶层增加 `private Integer schemaVersion = 1;` 及其 getters/setters。
- [ ] **Task 1.2**: 新增错误码 `PAGE_SCHEMA_VERSION_UNSUPPORTED(10012, "不支持的页面 Schema 版本")` 在 `ErrorCode.java` 中。
- [ ] **Task 1.3**: 创建 `PageSchemaUpgradeService` 接口及其实现类 `PageSchemaUpgradeServiceImpl` (位于 `modules/pagebuilder/service`)：
  * 定义常量 `public static final int CURRENT_SCHEMA_VERSION = 1;`
  * 方法 `PageSchemaModel upgradeToCurrent(PageSchemaModel rawSchema)`：实现 Legacy `v0` $\rightarrow$ `v1` 的平滑修正。
- [ ] **Task 1.4**: 更新 `PageSchemaValidationService`：
  * 在 `validateSchema(PageSchemaModel schema)` 入口处增加版本校验：若 `schema.getSchemaVersion() > CURRENT_SCHEMA_VERSION` 抛出 `10012` 异常；若为旧版本自动调用 `upgradeToCurrent` 完成升级。

---

### 阶段二：保存、预览、发布与回滚全链路贯通

- [ ] **Task 2.1**: 更新 `PageDraftServiceImpl`（保存草稿）、`PreviewTokenServiceImpl` / `PageRenderApplicationServiceImpl`（预览与渲染）、`PagePublishServiceImpl`（发布与回滚），确保在输入/加载点均执行版本升级与校验。
- [ ] **Task 2.2**: 同步更新 `docs/接口文档.md`，追加 `schemaVersion` 字段说明与 `10012` 错误码。

---

### 阶段三：自动化测试全覆盖

- [ ] **Task 3.1**: 创建 `PageSchemaUpgradeServiceTest.java` 单元测试：
  * 验证 `null` 版本 Legacy Schema 被自动纠正为 `v1`。
  * 验证未来未知版本（如 `v99`）触发 `10012` 异常。
- [ ] **Task 3.2**: 更新 `AdminPageDraftControllerTest` 与 `AdminPagePublishControllerTest`，覆盖带 `schemaVersion` 的正常与异常请求路径。

---

## 二、关键难点与解决办法速查表

| 难点序号 | 场景说明 | 核心风险 | 解决办法 |
| :---: | :--- | :--- | :--- |
| **N1** | 存量旧版数据缺失版本号 | 旧版本快照读取/回滚失败 | `upgradeToCurrent` 自动填充 `schemaVersion = 1` |
| **N2** | 前端尝试绕过版本控制提交高版本 | 破坏后端 Schema 解析结构 | 后端 `PageSchemaValidationService` 强校验 `> 1` 一律拒绝 |

---

## 三、文档自检核验 (Self-Inspection Verification)

在进入代码实现前，对照以下项进行阅读自检：

- [x] **检查项 1**：`schemaVersion` 模型定义与当前版本号常量是否明确？（已指定当前标准版本为 `1`）
- [x] **检查项 2**：`PageSchemaUpgradeService` 升级器机制是否具备可扩展性？（设计为渐进式升级方法）
- [x] **检查项 3**：保存、预览、发布、回滚四个关键节点是否均已纳入覆盖？（阶段二已列明）
- [x] **检查项 4**：错误码 `10012` 与 API 契约是否已规划对齐？（已指定错误码及文档同步要求）
- [x] **检查项 5**：是否满足 AGENTS.md 规范？（单元测试全覆盖、安全提示无内部泄露）

---

**自检结论**：自检完全通过，文档完整严密。
