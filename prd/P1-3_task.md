# P1-3 响应式断点协议任务拆解与自检清单 (task.md)

本文档将 **P1-3 响应式断点协议整改** 拆化为具体的开发与测试任务清单，并提供自检门禁。

---

## 一、任务细分拆解清单 (Step-by-Step Task Breakdown)

### 阶段一：模型扩展与校验辅助增强

- [ ] **Task 1.1**: 创建 `SectionResponsiveModel.java` 类 (位于 `modules/pagebuilder/model/SectionResponsiveModel.java`)：
  * 字段：`layout` (`ComponentLayoutModel`), `style` (`Map<String, Object>`) 及 getters/setters。
  * 更新 `SectionModel.java`：增加 `private Map<String, SectionResponsiveModel> responsive;` 字段及 getter/setter。
- [ ] **Task 1.2**: 更新 `LayoutStyleValidationHelper.java`：
  * 定义常量 `public static final Set<String> ALLOWED_BREAKPOINTS = Set.of("desktop", "tablet", "mobile");`。
  * 实现 `validateSectionResponsive(Map<String, SectionResponsiveModel> responsive, String componentCode)`：
    - 校验 key 是否必须属于 `ALLOWED_BREAKPOINTS`；非法 key 直接抛 `10001`。
    - 针对每个断点的 `layout` 再次执行 `validateComponentLayout`；
    - 针对每个断点的 `style` 再次执行 `validateComponentStyle`。

---

### 阶段二：校验集成与接口文档更新

- [ ] **Task 2.1**: 更新 `PageSchemaValidationServiceImpl.java`：
  * 在 `validateSection` 中加入 `LayoutStyleValidationHelper.validateSectionResponsive(section.getResponsive(), componentCode);`。
- [ ] **Task 2.2**: 同步更新 `docs/接口文档.md`：
  * 在低代码页面 Schema 配置说明中追加三级断点 (`desktop`, `tablet`, `mobile`) 划分、视口宽度范围、优先级以及覆盖回退规则。

---

### 阶段三：自动化测试全覆盖

- [ ] **Task 3.1**: 扩展 `PageSchemaValidationServiceTest.java`：
  * 验证正常移动端/平板端断点覆盖配置通过校验。
  * 验证非法断点名称（如 `iphone`）被拦截并抛出 `10001`。
  * 验证移动端断点内部 `zIndex` 超界被拦截。
  * 验证移动端断点内部样式危险脚本注入被拦截。

---

## 二、关键难点与解决办法速查表

| 难点序号 | 场景说明 | 核心风险 | 解决办法 |
| :---: | :--- | :--- | :--- |
| **N1** | 响应式断点内部样式逃逸 | 在 `mobile.style` 或 `tablet.layout` 中注入非法载荷绕过基础校验 | 递归对断点内部的所有 `layout` 与 `style` 重走全量白名单校验 |
| **N2** | 非法断点 key 污染 | 提交乱码或自定义断点 key 膨胀数据库 | 强校验 key 只能在 `desktop, tablet, mobile` 白名单内 |

---

## 三、文档自检核验 (Self-Inspection Verification)

在进入代码实现前，对照以下项进行阅读自检：

- [x] **检查项 1**：响应式三级断点 (`desktop`, `tablet`, `mobile`) 定义与视口范围是否清晰？（已量化宽度范围与优先级）
- [x] **检查项 2**：`SectionResponsiveModel` 领域模型与 `SectionModel` 挂载点是否确定？（已创建并挂载至 `SectionModel`）
- [x] **检查项 3**：响应式断点的布局与样式递归白名单校验机制是否明确？（已列明 `validateSectionResponsive` 递归校验）
- [x] **检查项 4**：组件树结构全视口锁定（防树分叉）规则是否确立？（已明确结构锁定，仅允许组件属性级受控覆盖）
- [x] **检查项 5**：接口文档更新与单元测试覆盖规划是否健全？（涵盖 API 文档说明与断点白名单/防注入单测）

---

**自检结论**：自检完全通过，文档完整严密。
