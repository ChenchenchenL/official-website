# P1-4 组件模板与绑定参数扩展任务拆解与自检清单 (task.md)

本文档将 **P1-4 组件模板与绑定参数扩展整改** 拆化为具体的开发与测试任务清单，并提供自检门禁。

---

## 一、任务细分拆解清单 (Step-by-Step Task Breakdown)

### 阶段一：绑定校验辅助工具与模型增强

- [ ] **Task 1.1**: 创建 `BindingQueryValidationHelper.java` (位于 `modules/pagebuilder/service/impl/BindingQueryValidationHelper.java`)：
  * 定义常量：`ALLOWED_QUERY_KEYS = Set.of("categoryId", "ids", "limit", "pageSize", "sortOrder", "orderBy");`。
  * 定义常量：`ALLOWED_SORT_ORDERS = Set.of("SORT_ASC", "SORT_DESC", "CREATE_TIME_DESC", "LATEST");`。
  * 实现 `validateBindingQuery(Map<String, Object> query, String componentCode)`：
    - 校验 Key 是否在 `ALLOWED_QUERY_KEYS` 内；非法 Key 直接抛 `10001`。
    - 校验 `ids` 大小上限 $\le 50$。
    - 校验 `limit` / `pageSize` 取值范围在 $[1, 50]$ 之间。
    - 校验 `sortOrder` / `orderBy` 属于 `ALLOWED_SORT_ORDERS`。
- [ ] **Task 1.2**: 更新 `ComponentTemplateVO.java`：
  * 增加 `getPropsSchema()`, `getStyleSchema()`, `getLayoutSchema()` 的结构化快捷 Getter，方便前端直接提取元数据生成属性面板。

---

### 阶段二：校验集成与接口文档更新

- [ ] **Task 2.1**: 更新 `PageSchemaValidationServiceImpl.java`：
  * 在 `validateSection` 中，当 `binding` 校验通过后，自动调用 `BindingQueryValidationHelper.validateBindingQuery(binding.getQuery(), componentCode);`。
- [ ] **Task 2.2**: 同步更新 `docs/接口文档.md`：
  * 补充首期 10 大组件集物料盘点表格及数据绑定参数白名单与限制规范。

---

### 阶段三：自动化测试全覆盖

- [ ] **Task 3.1**: 扩展 `PageSchemaValidationServiceTest.java`：
  * 验证合法 Binding query 配置（`categoryId: 1`, `limit: 10`, `sortOrder: "LATEST"`）正常通过。
  * 验证非法 query key（如 `customSQL`）被拦截并抛出 `10001`。
  * 验证 `ids` 超过 50 个被拦截并抛出 `10001`。
  * 验证 `limit` 超过 50 条被拦截并抛出 `10001`。
  * 验证非法排序枚举被拦截并抛出 `10001`。

---

## 二、关键难点与解决办法速查表

| 难点序号 | 场景说明 | 核心风险 | 解决办法 |
| :---: | :--- | :--- | :--- |
| **N1** | 绑定参数 SQL 注入与恶意查询 | 传入任意条件字段或拼装 SQL | 强校验 key 只能为 `categoryId, ids, limit, pageSize, sortOrder, orderBy` |
| **N2** | 大批量数据查询 DoS 攻击 | `ids` 传入几万个 ID 或 `limit=100000` | 强校验 `ids` 长度 $\le 50$，`limit` 范围在 $[1, 50]$ |

---

## 三、文档自检核验 (Self-Inspection Verification)

在进入代码实现前，对照以下项进行阅读自检：

- [x] **检查项 1**：首期 10 大核心组件集物料盘点与声明是否齐全？（已涵盖布局、文本、媒体、动态列表与表单）
- [x] **检查项 2**：`ComponentTemplateVO` 驱动前端属性面板元数据结构是否清晰？（已增加 propsSchema/styleSchema/layoutSchema 方法）
- [x] **检查项 3**：`BindingModel.query` 绑定筛选参数白名单与数量/ID上限是否明确？（已限定 5 大受控参数及 50 条/个上限）
- [x] **检查项 4**：SQL 注入防线与资源 DoS 拦截机制是否完备？（已建立强类型/白名单枚举/范围约束）
- [x] **检查项 5**：接口文档更新与单元测试覆盖规划是否健全？（涵盖接口文档说明与完整边界测试断言）

---

**自检结论**：自检完全通过，文档完整严密。
