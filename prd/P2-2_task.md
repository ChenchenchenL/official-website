# P2-2 版本差异与发布审阅任务拆解与自检清单 (task.md)

本文档将 **P2-2 版本差异与发布审阅整改** 拆化为具体的开发与测试任务清单，并提供自检门禁。

---

## 一、任务细分拆解清单 (Step-by-Step Task Breakdown)

### 阶段一：Diff 比对引擎与 VO 模型构建

- [x] **Task 1.1**: 创建 `SchemaDiffItemVO.java` (位于 `modules/pagebuilder/vo/SchemaDiffItemVO.java`)：
  * 包含 `path`, `componentId`, `componentCode`, `changeType`, `fieldName`, `oldValue`, `newValue` 等属性。
- [x] **Task 1.2**: 创建 `PublishReviewVO.java` (位于 `modules/pagebuilder/vo/PublishReviewVO.java`)：
  * 包含草稿与 ACTIVE快照 Hash/版本号、组件数对比、绑定源摘要、预校验结果与修改列表。
- [x] **Task 1.3**: 创建 `PageSchemaDiffHelper.java` (位于 `modules/pagebuilder/service/impl/PageSchemaDiffHelper.java`)：
  * 实现基于 Section `id` 的比对逻辑；提供敏感词脱敏过滤。

---

### 阶段二：核心业务服务实现

- [x] **Task 2.1**: 创建 `PageDiffService.java` 与 `PageDiffServiceImpl.java` (位于 `modules/pagebuilder/service/`)：
  * 实现 `comparePageSchema`: 计算草稿与指定版本/在线 ACTIVE 快照的变更明细列表。
  * 实现 `generatePublishReview`: 集成 Schema 预校验，生成包含绑定源摘要与变动列表的综合发布审阅模型。

---

### 阶段三：Admin API 控制器暴露与接口文档更新

- [x] **Task 3.1**: 创建 `AdminPageDiffController.java` (位于 `modules/pagebuilder/controller/AdminPageDiffController.java`)：
  * 暴露 `GET /admin/api/page-builder/pages/{pageId}/diff`
  * 暴露 `GET /admin/api/page-builder/pages/{pageId}/publish-review`
- [x] **Task 3.2**: 同步更新 `docs/接口文档.md`：
  * 补充上述 2 个 Admin 接口路径、参数、响应结构说明。

---

### 阶段四：自动化测试全覆盖

- [x] **Task 4.1**: 创建 `PageDiffServiceTest.java`：
  * 验证全新未发布页面的 Diff 比对（全量 ADDED）。
  * 验证增改组件属性时的 Diff 列表精准捕捉。
  * 验证敏感字段脱敏屏蔽。
  * 验证预校验报错捕获与发布阻断。

---

## 二、关键难点与解决办法速查表

| 难点序号 | 场景说明 | 核心风险 | 解决办法 |
| :---: | :--- | :--- | :--- |
| **N1** | 组件位置调整导致全量错位误报 | 按照数组 index 比较导致所有后续组件全报修改 | 基于组件 Section `id` 精确映射比较 |
| **N2** | 敏感配置泄露风险 | 泄露服务器真实路径或内部密钥凭证 | 建立敏感属性黑名单掩码过滤机制 |

---

## 三、文档自检核验 (Self-Inspection Verification)

在进入代码实现前，对照以下项进行阅读自检：

- [x] **检查项 1**：后端 Schema Diff 计算与发布审阅方案选型是否明确？（已选定后端安全脱敏 Diff + 发布预审概览方案）
- [x] **检查项 2**：`SchemaDiffItemVO` 与 `PublishReviewVO` 领域模型结构是否健全？（已覆盖组件、字段、新旧值、Hash、预校验与绑定源摘要）
- [x] **检查项 3**：基于 Section ID 的节点比对算法与顺序调整匹配逻辑是否清晰？（已确立按 ID 构建索引比对，防止位置错位误报）
- [x] **检查项 4**：敏感属性过滤与掩码脱敏防线是否健全？（建立了包含敏感 key 的过滤屏蔽机制）
- [x] **检查项 5**：接口文档更新与单元测试计划是否完备？（涵盖 API 文档说明与深层比对/脱敏/预校验测试）

---

**自检结论**：自检完全通过，文档完整严密。
